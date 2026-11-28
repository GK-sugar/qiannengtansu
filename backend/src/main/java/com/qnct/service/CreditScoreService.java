package com.qnct.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qnct.entity.EntActivityData;
import com.qnct.entity.EntRectifyIssue;
import com.qnct.entity.FinCreditProfile;
import com.qnct.entity.GovAnomalyAlert;
import com.qnct.mapper.CarbonPassportMapper;
import com.qnct.mapper.EntActivityDataMapper;
import com.qnct.mapper.EntRectifyIssueMapper;
import com.qnct.mapper.FinCreditProfileMapper;
import com.qnct.mapper.GovAnomalyAlertMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 绿色金融：F7 企业低碳信誉分 + F8 预授信额度。
 * 正式评分直接读取活动数据（完整度/稳定性）、异常预警（历史合规）、整改闭环（履约），
 * 结果回写 fin_credit_profile 作为档案快照；数据不可用时回退演示参数。
 */
@Slf4j
@Service
public class CreditScoreService {

    private final EntActivityDataMapper activityMapper;
    private final GovAnomalyAlertMapper alertMapper;
    private final EntRectifyIssueMapper issueMapper;
    private final CarbonPassportMapper passportMapper;
    private final FinCreditProfileMapper profileMapper;
    private final ObjectMapper objectMapper;

    public CreditScoreService(EntActivityDataMapper activityMapper, GovAnomalyAlertMapper alertMapper,
                              EntRectifyIssueMapper issueMapper, CarbonPassportMapper passportMapper,
                              FinCreditProfileMapper profileMapper, ObjectMapper objectMapper) {
        this.activityMapper = activityMapper;
        this.alertMapper = alertMapper;
        this.issueMapper = issueMapper;
        this.passportMapper = passportMapper;
        this.profileMapper = profileMapper;
        this.objectMapper = objectMapper;
    }

    /** F7：信誉分 = 合规得分×0.3 + 数据质量×0.25 + 减排绩效×0.25 + 履约记录×0.2 */
    public int score(int compliance, int dataQuality, int reduction, int fulfillment) {
        double s = compliance * 0.3 + dataQuality * 0.25 + reduction * 0.25 + fulfillment * 0.2;
        return Math.min(100, Math.max(0, (int) Math.round(s)));
    }

    /** F8：预授信 = 年营收基数 × 信誉分/100 × 0.8 */
    public long creditLimit(long annualBase, int score) {
        return Math.round(annualBase * (score / 100.0) * 0.8);
    }

    public Map<String, Object> profile(int compliance, int dataQuality, int reduction, int fulfillment, long annualBase) {
        int s = score(compliance, dataQuality, reduction, fulfillment);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("score", s);
        m.put("creditLimit", creditLimit(annualBase, s));
        Map<String, Object> radar = new LinkedHashMap<>();
        radar.put("合规", compliance);
        radar.put("数据质量", dataQuality);
        radar.put("减排绩效", reduction);
        radar.put("履约", fulfillment);
        m.put("radar", radar);
        return m;
    }

    /** 按企业实时数据评分（前端 finance 页使用） */
    public Map<String, Object> profileForOrg(Long orgId, long annualBase) {
        List<EntActivityData> rows = new ArrayList<>();
        try {
            rows = activityMapper.selectList(new LambdaQueryWrapper<EntActivityData>()
                    .eq(EntActivityData::getOrgId, orgId)
                    .orderByDesc(EntActivityData::getPeriod)
                    .last("limit 2000"));
        } catch (Exception e) {
            log.warn("信誉分读取活动数据失败，使用演示参数：{}", e.getMessage());
        }

        int completeness = 92;
        int timeliness = 88;
        int stability = 81;
        int compliance = 95;
        int fulfillment = 95;

        if (!rows.isEmpty()) {
            List<String> periods = new ArrayList<>(rows.stream()
                    .map(EntActivityData::getPeriod)
                    .filter(p -> p != null && !p.isBlank())
                    .distinct().toList());
            periods.sort(String::compareTo);
            // 数据完整度：近 3 个核算期是否连续上报（与填报周期节奏一致）
            int recent = Math.min(3, periods.size());
            completeness = clamp((int) Math.round(100.0 * recent / 3), 0, 100);

            long filled = rows.stream().filter(r -> r.getEmissionCo2e() != null).count();
            completeness = (int) Math.round(completeness * 0.7 + filled * 100.0 / rows.size() * 0.3);

            long confirmed = rows.stream().filter(r -> "CONFIRMED".equals(r.getDataStatus())
                    || "VERIFIED".equals(r.getDataStatus())).count();
            timeliness = rows.isEmpty() ? 88 : clamp((int) Math.round(confirmed * 100.0 / rows.size()), 40, 100);

            Map<String, BigDecimal> byPeriod = new TreeMap<>();
            for (EntActivityData r : rows) {
                if (r.getPeriod() != null) {
                    byPeriod.merge(r.getPeriod(),
                            r.getEmissionCo2e() == null ? BigDecimal.ZERO : r.getEmissionCo2e(), BigDecimal::add);
                }
            }
            // 稳定性只比较已结束月份，避免当月未结数据造成虚高波动
            String current = LocalDate.now().toString().substring(0, 7);
            List<BigDecimal> totals = byPeriod.entrySet().stream()
                    .filter(e -> !e.getKey().equals(current))
                    .map(Map.Entry::getValue).toList();
            if (totals.size() >= 2) {
                double changeSum = 0;
                int changeCount = 0;
                for (int i = 1; i < totals.size(); i++) {
                    BigDecimal prev = totals.get(i - 1);
                    if (prev.compareTo(BigDecimal.ZERO) > 0) {
                        changeSum += Math.abs(totals.get(i).subtract(prev).doubleValue() / prev.doubleValue());
                        changeCount++;
                    }
                }
                double avgChange = changeCount == 0 ? 0.15 : changeSum / changeCount;
                stability = clamp((int) Math.round(100 - avgChange * 120), 55, 100);
            } else {
                stability = 82;
            }
        }

        long openAlerts = -1;
        long openIssues = -1;
        long closedIssues = 0;
        try {
            openAlerts = alertMapper.selectCount(new LambdaQueryWrapper<GovAnomalyAlert>()
                    .eq(GovAnomalyAlert::getOrgId, orgId)
                    .eq(GovAnomalyAlert::getStatus, "PENDING"));
        } catch (Exception ignored) {
        }
        try {
            List<EntRectifyIssue> issues = issueMapper.selectList(new LambdaQueryWrapper<EntRectifyIssue>()
                    .eq(EntRectifyIssue::getOrgId, orgId));
            openIssues = issues.stream().filter(i -> !isClosed(i.getStatus())).count();
            closedIssues = issues.stream().filter(i -> isClosed(i.getStatus())).count();
        } catch (Exception ignored) {
        }
        if (openAlerts >= 0 || openIssues >= 0) {
            compliance = clamp((int) Math.round(100
                    - Math.max(0, openAlerts) * 7
                    - Math.max(0, openIssues) * 5), 45, 100);
        }
        if (openIssues >= 0 && openIssues + closedIssues > 0) {
            fulfillment = clamp((int) Math.round(closedIssues * 100.0 / (openIssues + closedIssues)), 40, 100);
        }

        int s = (int) Math.round(completeness * 0.4 + timeliness * 0.25 + stability * 0.2 + compliance * 0.15);
        String level = s >= 90 ? "AAA" : s >= 80 ? "AA" : s >= 70 ? "A" : "B";
        double k = s >= 90 ? 1.0 : s >= 80 ? 0.85 : s >= 70 ? 0.7 : 0.5;
        BigDecimal baseQuota = BigDecimal.valueOf(annualBase).divide(new BigDecimal("10000"), 2, RoundingMode.HALF_UP);
        BigDecimal creditQuota = baseQuota.multiply(BigDecimal.valueOf(k)).setScale(2, RoundingMode.HALF_UP);
        long limit = creditQuota.multiply(new BigDecimal("10000")).longValue();

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("数据完整度", completeness);
        detail.put("及时提交率", timeliness);
        detail.put("核算稳定性", stability);
        detail.put("历史合规", compliance);

        Map<String, Object> radar = new LinkedHashMap<>();
        radar.put("合规", compliance);
        radar.put("数据质量", completeness);
        radar.put("减排绩效", stability);
        radar.put("履约", fulfillment);

        int applyCount = 0;
        try {
            Long c = passportMapper.selectCount(new LambdaQueryWrapper<com.qnct.entity.CarbonPassport>()
                    .eq(com.qnct.entity.CarbonPassport::getOrgId, orgId));
            applyCount = c == null ? 0 : c.intValue();
        } catch (Exception ignored) {
        }

        saveProfile(orgId, s, level, detail, baseQuota, creditQuota, rows.size(), applyCount);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orgId", orgId);
        m.put("score", s);
        m.put("level", level);
        m.put("creditLimit", limit);
        m.put("baseQuotaWan", baseQuota);
        m.put("creditQuotaWan", creditQuota);
        m.put("historyCount", rows.size());
        m.put("applyCount", applyCount);
        m.put("calcTime", LocalDateTime.now());
        m.put("radar", radar);
        m.put("detail", detail);
        m.put("levelCoefficient", k);
        m.put("formula", "F7 S = 40×完整度 + 25×及时性 + 20×稳定性 + 15×历史合规；F8 Q = Q基准 × k等级");
        return m;
    }

    private boolean isClosed(String status) {
        return "CLOSED".equalsIgnoreCase(status) || "已闭环".equals(status);
    }

    private void saveProfile(Long orgId, int s, String level, Map<String, Object> detail,
                             BigDecimal baseQuota, BigDecimal creditQuota, int history, int applies) {
        try {
            FinCreditProfile p = profileMapper.selectOne(new LambdaQueryWrapper<FinCreditProfile>()
                    .eq(FinCreditProfile::getOrgId, orgId).last("limit 1"));
            boolean insert = p == null;
            if (insert) {
                p = new FinCreditProfile();
                p.setOrgId(orgId);
                p.setCreateTime(LocalDateTime.now());
            }
            p.setScore(BigDecimal.valueOf(s));
            p.setLevel(level);
            p.setScoreDetailJson(objectMapper.writeValueAsString(detail));
            p.setBaseQuota(baseQuota);
            p.setCreditQuota(creditQuota);
            p.setHistoryCount(history);
            p.setApplyCount(applies);
            p.setCalcTime(LocalDateTime.now());
            p.setUpdateTime(LocalDateTime.now());
            if (insert) {
                profileMapper.insert(p);
            } else {
                profileMapper.updateById(p);
            }
        } catch (Exception e) {
            log.warn("信誉档案回写失败：{}", e.getMessage());
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}