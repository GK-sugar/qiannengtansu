package com.qnct.controller.enterprise;

import com.qnct.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业端 · 一键核算报告（ECharts 图表数据 + 月度趋势）
 */
@RestController
@RequestMapping("/api/v1/enterprise/report")
public class ReportController {

    @GetMapping("/{period}")
    public Result<Map<String, Object>> report(@PathVariable String period) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("period", period);
        m.put("title", "贵州黔能矿业 " + period + " 碳排放核算报告");
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
        return Result.ok(m);
    }

    @PostMapping("/download")
    public Result<Map<String, Object>> download(@RequestBody Map<String, String> body) {
        return Result.ok(Map.of("file", "/files/report/QR-" + body.getOrDefault("period", "2026-10") + ".pdf",
                "note", "报告模板渲染（本地模板）+ 电子签章"));
    }
}