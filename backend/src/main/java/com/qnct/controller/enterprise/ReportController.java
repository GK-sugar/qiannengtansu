package com.qnct.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qnct.common.Result;
import com.qnct.entity.CarbonReport;
import com.qnct.entity.EntActivityData;
import com.qnct.mapper.CarbonReportMapper;
import com.qnct.mapper.EntActivityDataMapper;
import com.qnct.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 企业端 · 一键核算报告（ECharts 图表数据 + 月度趋势）
 * 总量/范围占比/趋势/排放源排名全部由 ent_activity_data 实时聚合；
 * 碳强度优先取 carbon_report 归档值。库不可用时降级演示数据。
 */
@RestController
@RequestMapping("/api/v1/enterprise/report")
public class ReportController {

    private final EntActivityDataMapper activityMapper;
    private final CarbonReportMapper reportMapper;

    public ReportController(EntActivityDataMapper activityMapper, CarbonReportMapper reportMapper) {
        this.activityMapper = activityMapper;
        this.reportMapper = reportMapper;
    }

    @GetMapping("/{period}")
    public Result<Map<String, Object>> report(@PathVariable String period) {
        Long orgId = SecurityUtils.orgId(1001L);
        var login = SecurityUtils.current();
        String orgName = login != null && login.getOrgName() != null ? login.getOrgName() : "贵州黔能矿业";
        try {
            List<EntActivityData> all = activityMapper.selectList(new LambdaQueryWrapper<EntActivityData>()
                    .eq(EntActivityData::getOrgId, orgId));
            List<EntActivityData> rows = all.stream()
                    .filter(r -> period.equals(r.getPeriod()))
                    .toList();
            if (!rows.isEmpty()) {
                return Result.ok(build(period, orgName, orgId, rows, all));
            }
        } catch (Exception ignored) {
        }
        return Result.ok(demo(period, orgName));
    }

    private Map<String, Object> build(String period, String orgName, Long orgId,
                                      List<EntActivityData> rows, List<EntActivityData> all) {
        BigDecimal totalKg = sum(rows);
        BigDecimal s1 = sum(rows.stream().filter(r -> "1".equals(r.getScope())).toList());
        BigDecimal s2 = sum(rows.stream().filter(r -> "2".equals(r.getScope())).toList());
        BigDecimal s3 = sum(rows.stream().filter(r -> "3".equals(r.getScope())).toList());

        Map<String, BigDecimal> byPeriod = new TreeMap<>();
        for (EntActivityData r : all) {
            if (r.getPeriod() == null) {
                continue;
            }
            byPeriod.merge(r.getPeriod(), nz(r.getEmissionCo2e()), BigDecimal::add);
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        List<String> periods = new ArrayList<>(byPeriod.keySet());
        int from = Math.max(0, periods.size() - 6);
        for (String p : periods.subList(from, periods.size())) {
            trend.add(Map.of("month", p, "value", t(byPeriod.get(p))));
        }

        Map<String, BigDecimal> bySource = new LinkedHashMap<>();
        for (EntActivityData r : rows) {
            bySource.merge(r.getActivityName() == null ? "其他" : r.getActivityName(), nz(r.getEmissionCo2e()), BigDecimal::add);
        }
        List<Map<String, Object>> rank = bySource.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(e -> Map.<String, Object>of("source", e.getKey(), "value", t(e.getValue())))
                .toList();

        List<Map<String, Object>> pie = new ArrayList<>();
        addPie(pie, "范围一 · 化石燃料", s1);
        addPie(pie, "范围二 · 外购电力", s2);
        addPie(pie, "范围三 · 运输", s3);

        BigDecimal intensity = intensity(orgId, period);
        if (intensity == null) {
            intensity = totalKg.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : totalKg.divide(new BigDecimal("1200000"), 6, RoundingMode.HALF_UP);
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("period", period);
        m.put("title", orgName + " " + period + " 碳排放核算报告");
        m.put("total", t(totalKg));
        m.put("scope1", t(s1));
        m.put("scope2", t(s2));
        m.put("scope3", t(s3));
        m.put("intensity", intensity.doubleValue());
        m.put("pie", pie);
        m.put("trend", trend);
        m.put("rank", rank);
        m.put("source", "DB");
        m.put("recordCount", rows.size());
        m.put("note", "按 ent_activity_data 活动数据实时聚合（kgCO2e → tCO2e），碳强度取 carbon_report 归档值");
        return m;
    }

    private BigDecimal intensity(Long orgId, String period) {
        try {
            CarbonReport r = reportMapper.selectOne(new LambdaQueryWrapper<CarbonReport>()
                    .eq(CarbonReport::getOrgId, orgId)
                    .eq(CarbonReport::getPeriod, period)
                    .isNotNull(CarbonReport::getIntensity)
                    .orderByDesc(CarbonReport::getCreateTime)
                    .last("limit 1"));
            return r == null ? null : r.getIntensity();
        } catch (Exception e) {
            return null;
        }
    }

    private void addPie(List<Map<String, Object>> pie, String name, BigDecimal kg) {
        if (kg.compareTo(BigDecimal.ZERO) > 0) {
            pie.add(Map.of("name", name, "value", t(kg)));
        }
    }

    private Map<String, Object> demo(String period, String orgName) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("period", period);
        m.put("title", orgName + " " + period + " 碳排放核算报告");
        m.put("total", 7380.65);
        m.put("scope1", 2114.36);
        m.put("scope2", 4808.90);
        m.put("scope3", 457.39);
        m.put("intensity", 0.1842);
        m.put("pie", List.of(
                Map.of("name", "范围一 · 化石燃料", "value", 2114.36),
                Map.of("name", "范围二 · 外购电力", "value", 4808.90),
                Map.of("name", "范围三 · 运输", "value", 457.39)));
        m.put("trend", List.of(
                Map.of("month", "2026-05", "value", 6941.2),
                Map.of("month", "2026-06", "value", 7114.5),
                Map.of("month", "2026-07", "value", 7410.3),
                Map.of("month", "2026-08", "value", 7203.1),
                Map.of("month", "2026-09", "value", 7611.8),
                Map.of("month", "2026-10", "value", 7380.65)));
        m.put("rank", List.of(
                Map.of("source", "电力", "value", 4808.90),
                Map.of("source", "原煤", "value", 1609.22),
                Map.of("source", "柴油", "value", 505.14),
                Map.of("source", "公路运输", "value", 457.39)));
        m.put("source", "DEMO");
        return m;
    }

    private static BigDecimal sum(List<EntActivityData> rows) {
        BigDecimal s = BigDecimal.ZERO;
        for (EntActivityData r : rows) {
            s = s.add(nz(r.getEmissionCo2e()));
        }
        return s;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** kgCO2e → tCO2e，保留两位 */
    private static double t(BigDecimal kg) {
        return kg.divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP).doubleValue();
    }

    @PostMapping("/download")
    public Result<Map<String, Object>> download(@RequestBody Map<String, String> body) {
        return Result.ok(Map.of("file", "/files/report/QR-" + body.getOrDefault("period", "2026-10") + ".pdf",
                "note", "报告模板渲染（本地模板）+ 电子签章"));
    }
}