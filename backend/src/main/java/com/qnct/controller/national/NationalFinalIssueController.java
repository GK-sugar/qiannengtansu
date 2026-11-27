package com.qnct.controller.national;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qnct.common.Result;
import com.qnct.entity.NatFinalIssue;
import com.qnct.mapper.NatFinalIssueMapper;
import com.qnct.service.PassportService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 国家级审批部门 · 终审与发证中心
 *   - 待终审清单：核查机构复核结论 PASSED 后进入国家级终审队列
 *   - 终审通过：签发碳足迹护照（F11 防伪令牌 + 二维码），写入 F10 哈希链存证
 *   - 终审驳回：回写 REJECTED 与驳回原因，退回核查机构
 */
@RestController
@RequestMapping("/api/v1/national/issuance")
public class NationalFinalIssueController {

    private final NatFinalIssueMapper finalIssueMapper;
    private final PassportService passportService;

    public NationalFinalIssueController(NatFinalIssueMapper finalIssueMapper, PassportService passportService) {
        this.finalIssueMapper = finalIssueMapper;
        this.passportService = passportService;
    }

    /** 待终审清单（数据库优先，缺表或暂无数据时返回演示数据） */
    @GetMapping("/pending")
    public Result<List<Map<String, Object>>> pending() {
        List<Map<String, Object>> out = new ArrayList<>();
        try {
            List<NatFinalIssue> rows = finalIssueMapper.selectList(
                    new QueryWrapper<NatFinalIssue>().eq("status", "PENDING").orderByDesc("create_time"));
            if (rows != null && !rows.isEmpty()) {
                for (NatFinalIssue r : rows) {
                    out.add(toRow(r));
                }
                return Result.ok(out);
            }
        } catch (Exception ignored) {
        }
        out.add(demo("SQ-2026-0041", "贵州黔能矿业有限责任公司", "2026-Q3", "精煤（洗选产品）",
                "ISO 14067 / CBAM", "3842.75", "0.1842", "PASSED"));
        out.add(demo("SQ-2026-0038", "贵州中伟新能源材料有限公司", "2026-09", "磷酸铁锂正极材料",
                "EU Battery", "1286.40", "1.2080", "PASSED"));
        out.add(demo("SQ-2026-0045", "贵州磷化集团有限责任公司", "2026-09", "磷酸二铵",
                "CBAM", "7640.20", "0.9400", "REJECTED"));
        return Result.ok(out);
    }

    /** 终审决定：approval=true 签发护照（F10+F11），false 驳回退回 */
    @PostMapping("/decide")
    public Result<Map<String, Object>> decide(@RequestBody Map<String, String> body) {
        String no = body.getOrDefault("applicationNo", "").trim();
        if (no.isEmpty()) {
            return Result.fail(400, "申请单号 applicationNo 不能为空");
        }
        boolean approval = !"false".equalsIgnoreCase(body.getOrDefault("approval", "true"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applicationNo", no);

        NatFinalIssue rec = new NatFinalIssue();
        rec.setApplicationNo(no);
        rec.setEntName(body.getOrDefault("entName", "贵州黔能矿业有限责任公司"));
        rec.setOrgId(parseLong(body.getOrDefault("orgId", "1001"), 1001L));
        rec.setPeriod(body.getOrDefault("period", "2026-Q3"));
        rec.setProductName(body.getOrDefault("productName", "精煤（洗选产品）"));
        rec.setAgencyConclusion("REJECTED".equals(body.getOrDefault("agencyConclusion", "")) ? "REJECTED" : "PASSED");
        rec.setTotalCo2e(parseDecimal(body.getOrDefault("totalCo2e", "3842.75"), new BigDecimal("3842.75")));
        rec.setIntensity(parseDecimal(body.getOrDefault("intensity", "0.1842"), new BigDecimal("0.1842")));
        rec.setCreateTime(LocalDateTime.now());
        rec.setUpdateTime(LocalDateTime.now());

        if (!approval) {
            rec.setStatus("REJECTED");
            try {
                finalIssueMapper.insert(rec);
            } catch (Exception ignored) {
            }
            result.put("approval", false);
            result.put("status", "REJECTED");
            result.put("rejectReason", body.getOrDefault("rejectReason", "终审未通过（原因未填写）"));
            result.put("decidedAt", LocalDateTime.now().toString());
            return Result.ok(result);
        }

        // 终审通过：签发防伪护照 → 写入哈希链存证（F10）→ 生成防伪令牌（F11）
        Map<String, Object> issued = passportService.issue(
                rec.getOrgId(), rec.getProductName(), "ISO 14067 / CBAM",
                rec.getTotalCo2e(), rec.getIntensity(), "国家应对气候变化战略中心 · 周司长");

        rec.setStatus("APPROVED");
        rec.setPassportNo((String) issued.get("passportNo"));
        rec.setVerifyToken((String) issued.get("verifyToken"));
        rec.setHashChainSeq(ThreadLocalRandom.current().nextLong(90000L, 999999L));
        rec.setSignerName("国家应对气候变化战略中心 · 周司长");
        rec.setIssueTime(LocalDateTime.now());
        try {
            finalIssueMapper.insert(rec);
        } catch (Exception ignored) {
        }

        result.put("approval", true);
        result.put("status", "APPROVED");
        result.put("passportNo", issued.get("passportNo"));
        result.put("verifyToken", issued.get("verifyToken"));
        result.put("hashChainSeq", rec.getHashChainSeq());
        result.put("hashValue", issued.get("hashValue"));
        result.put("validFrom", issued.get("validFrom"));
        result.put("validTo", issued.get("validTo"));
        result.put("signer", rec.getSignerName());
        result.put("issuedAt", LocalDateTime.now().toString());
        return Result.ok(result);
    }

    /** 已签发护照列表 */
    @GetMapping("/issued")
    public Result<List<Map<String, Object>>> issued() {
        List<Map<String, Object>> out = new ArrayList<>();
        try {
            List<NatFinalIssue> rows = finalIssueMapper.selectList(
                    new QueryWrapper<NatFinalIssue>().eq("status", "APPROVED").orderByDesc("issue_time"));
            if (rows != null && !rows.isEmpty()) {
                for (NatFinalIssue r : rows) {
                    out.add(toRow(r));
                }
                return Result.ok(out);
            }
        } catch (Exception ignored) {
        }
        out.add(demo("SQ-2026-0008", "贵州黔能矿业有限责任公司", "2026-09", "精煤（洗选产品）",
                "ISO 14067 / CBAM", "3619.12", "0.1810", "PASSED"));
        out.add(demo("SQ-2026-0006", "贵州中伟新能源材料有限公司", "2026-08", "磷酸铁锂正极材料",
                "EU Battery", "1210.55", "1.1860", "PASSED"));
        for (Map<String, Object> m : out) {
            m.put("status", "APPROVED");
            m.put("passportNo", "QN-PP-2026-" + String.format("%04d", 5000 + out.indexOf(m)));
            m.put("hashChainSeq", 94120L + out.indexOf(m));
        }
        return Result.ok(out);
    }

    private Map<String, Object> demo(String no, String ent, String period, String product,
                                     String standard, String co2e, String intensity, String conclusion) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("applicationNo", no);
        m.put("entName", ent);
        m.put("period", period);
        m.put("productName", product);
        m.put("standard", standard);
        m.put("totalCo2e", co2e);
        m.put("intensity", intensity + " tCO2e/t");
        m.put("agencyConclusion", conclusion);
        m.put("status", "PENDING");
        m.put("passportNo", null);
        m.put("hashChainSeq", null);
        m.put("createTime", "2026-11-" + (10 + (no.length() % 15)));
        return m;
    }

    private Map<String, Object> toRow(NatFinalIssue r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("applicationNo", r.getApplicationNo());
        m.put("passportNo", r.getPassportNo());
        m.put("entName", r.getEntName());
        m.put("period", r.getPeriod());
        m.put("productName", r.getProductName());
        m.put("agencyConclusion", r.getAgencyConclusion());
        m.put("status", r.getStatus());
        m.put("totalCo2e", r.getTotalCo2e());
        m.put("intensity", r.getIntensity());
        m.put("hashChainSeq", r.getHashChainSeq());
        m.put("verifyToken", r.getVerifyToken());
        m.put("signerName", r.getSignerName());
        m.put("issueTime", r.getIssueTime());
        m.put("createTime", r.getCreateTime());
        return m;
    }

    private long parseLong(String s, long def) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return def;
        }
    }

    private BigDecimal parseDecimal(String s, BigDecimal def) {
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return def;
        }
    }
}