package com.qnct.controller.national;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qnct.common.Result;
import com.qnct.entity.NatTariffRule;
import com.qnct.mapper.NatTariffRuleMapper;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 国家级审批部门 · 国际关税预警
 *   - CBAM（欧盟碳边境调节机制）税率规则库维护
 *   - 影响测算：输入 行业/单位产品碳排放/出口量，输出碳关税影响额（EUR 与 CNY）
 *   纯内置规则推演，不需要任何外部 API Key。
 */
@RestController
@RequestMapping("/api/v1/national/tariff")
public class NationalTariffController {

    private final NatTariffRuleMapper tariffRuleMapper;

    /** 内置 CBAM 基准规则（EUR/tCO2e = 欧盟碳价；freeAllowance = 免费配额%） */
    private static final List<Map<String, Object>> DEFAULT_RULES = List.of(
            rule("钢铁", "粗钢、钢材轧制产品（CN 72）", "71.50", "97.5", "2034 年免费配额逐年退坡至 0"),
            rule("铝", "原铝及铝制品（CN 76）", "71.50", "97.5", "直接排放纳入，间接排放暂缓"),
            rule("水泥", "水泥熟料及水泥（CN 2523）", "71.50", "97.5", "2026 年全面执行，豁免叠加电耗核算"),
            rule("化肥", "氮肥及部分磷肥（CN 31）", "71.20", "96.0", "出口体量较小但单位排放高"),
            rule("电力", "并网直购电力（间接排放）", "71.50", "0", "欧洲购电协议 PPA 可抵扣"),
            rule("氢", "灰氢与部分蓝氢（CN 2804）", "70.90", "95.0", "绿氢占比逐年提升"));

    public NationalTariffController(NatTariffRuleMapper tariffRuleMapper) {
        this.tariffRuleMapper = tariffRuleMapper;
    }

    /** 税率规则库（数据库优先，缺表时返回内置规则） */
    @GetMapping("/rules")
    public Result<List<Map<String, Object>>> rules() {
        List<Map<String, Object>> out = new ArrayList<>();
        try {
            List<NatTariffRule> rows = tariffRuleMapper.selectList(
                    new QueryWrapper<NatTariffRule>().eq("status", "ACTIVE").orderByAsc("id"));
            if (rows != null && !rows.isEmpty()) {
                for (NatTariffRule r : rows) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("industry", r.getIndustry());
                    m.put("productScope", r.getProductScope());
                    m.put("tariffRate", r.getTariffRate());
                    m.put("freeAllowance", r.getFreeAllowance());
                    m.put("effectiveFrom", r.getEffectiveFrom());
                    m.put("note", r.getNote());
                    m.put("status", r.getStatus());
                    out.add(m);
                }
                return Result.ok(out);
            }
        } catch (Exception ignored) {
        }
        return Result.ok(new ArrayList<>(DEFAULT_RULES));
    }

    /** 新增 / 更新关税规则（管理员维护） */
    @PostMapping("/rules")
    public Result<Map<String, Object>> addRule(@RequestBody Map<String, String> body) {
        NatTariffRule r = new NatTariffRule();
        r.setIndustry(body.getOrDefault("industry", "").trim());
        r.setProductScope(body.getOrDefault("productScope", ""));
        r.setTariffRate(parseDecimal(body.getOrDefault("tariffRate", "71.50"), new BigDecimal("71.50")));
        r.setFreeAllowance(parseDecimalAllowance(body.getOrDefault("freeAllowance", "97.5")));
        try {
            r.setEffectiveFrom(LocalDate.parse(body.getOrDefault("effectiveFrom", LocalDate.now().toString())));
        } catch (Exception e) {
            r.setEffectiveFrom(LocalDate.now());
        }
        r.setNote(body.getOrDefault("note", ""));
        r.setStatus("ACTIVE");
        r.setCreateTime(LocalDateTime.now());
        r.setUpdateTime(LocalDateTime.now());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("industry", r.getIndustry());
        m.put("tariffRate", r.getTariffRate());
        m.put("freeAllowance", r.getFreeAllowance());
        try {
            tariffRuleMapper.insert(r);
            m.put("saved", true);
        } catch (Exception e) {
            m.put("saved", false);
        }
        m.put("id", r.getId());
        return Result.ok(m);
    }

    /** 关税影响测算：排放 ×（1 - 免费配额）× 税率 */
    @PostMapping("/impact")
    public Result<Map<String, Object>> impact(@RequestBody Map<String, String> body) {
        String industry = body.getOrDefault("industry", "钢铁").trim();
        BigDecimal emission = parseDecimal(body.getOrDefault("emissionPerTon", "1.80"), new BigDecimal("1.80"));
        BigDecimal qty = parseDecimal(body.getOrDefault("exportQty", "10000"), new BigDecimal("10000"));

        BigDecimal rate = new BigDecimal("71.50");
        BigDecimal free = new BigDecimal("97.5");
        try {
            List<NatTariffRule> rows = tariffRuleMapper.selectList(
                    new QueryWrapper<NatTariffRule>().eq("industry", industry)
                            .eq("status", "ACTIVE").orderByDesc("effective_from").last("LIMIT 1"));
            if (rows != null && !rows.isEmpty()) {
                rate = rows.get(0).getTariffRate();
                free = rows.get(0).getFreeAllowance();
            }
        } catch (Exception ignored) {
            for (Map<String, Object> d : DEFAULT_RULES) {
                if (industry.equals(d.get("industry"))) {
                    rate = new BigDecimal(String.valueOf(d.get("tariffRate")));
                    free = new BigDecimal(String.valueOf(d.get("freeAllowance")));
                    break;
                }
            }
        }

        BigDecimal totalEmission = emission.multiply(qty).setScale(2, RoundingMode.HALF_UP);
        BigDecimal freePct = free.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        BigDecimal taxable = totalEmission.multiply(BigDecimal.ONE.subtract(freePct)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountEur = taxable.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cnyRate = new BigDecimal("7.85");
        BigDecimal amountCny = amountEur.multiply(cnyRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal perTonCny = qty.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : amountCny.divide(qty, 2, RoundingMode.HALF_UP);

        String risk = perTonCny.compareTo(new BigDecimal("120")) >= 0 ? "HIGH"
                : perTonCny.compareTo(new BigDecimal("40")) >= 0 ? "MEDIUM" : "LOW";
        String suggest = switch (risk) {
            case "HIGH" -> "碳成本已侵蚀出口毛利，建议优先采购绿电、切换低碳工艺并申请零碳追溯标识";
            case "MEDIUM" -> "建议建立一级供应商核算协同，按季度跟踪欧盟碳价与免费配额退坡";
            default -> "风险可控，持续维护 CBAM 报告即可";
        };

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("industry", industry);
        m.put("emissionPerTon", emission);
        m.put("exportQty", qty);
        m.put("tariffRate", rate);
        m.put("freeAllowance", free);
        m.put("totalEmission", totalEmission);
        m.put("taxableEmission", taxable);
        m.put("amountEur", amountEur);
        m.put("amountCny", amountCny);
        m.put("perTonCny", perTonCny);
        m.put("risk", risk);
        m.put("suggest", suggest);
        return Result.ok(m);
    }

    private static Map<String, Object> rule(String industry, String scope, String rate, String free, String note) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("industry", industry);
        m.put("productScope", scope);
        m.put("tariffRate", new BigDecimal(rate));
        m.put("freeAllowance", new BigDecimal(free));
        m.put("effectiveFrom", "2026-01-01");
        m.put("note", note);
        m.put("status", "ACTIVE");
        return m;
    }

    private BigDecimal parseDecimal(String s, BigDecimal def) {
        try {
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private BigDecimal parseDecimalAllowance(String s) {
        BigDecimal d = parseDecimal(s, new BigDecimal("97.5"));
        if (d.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (d.compareTo(new BigDecimal("100")) > 0) {
            return new BigDecimal("100");
        }
        return d;
    }
}