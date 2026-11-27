package com.qnct.controller.enterprise;

import com.qnct.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 企业端 · 供应链协同：供应商档案 / 物料流登记 / 协同任务
 */
@RestController
@RequestMapping("/api/v1/enterprise/supply")
public class SupplierController {

    @GetMapping("/suppliers")
    public Result<List<Map<String, Object>>> suppliers() {
        return Result.ok(List.of(
                Map.of("name", "贵州盘江供应链有限公司", "category", "原煤供应商", "credit", "A", "validUntil", "2027-06-30"),
                Map.of("name", "南方电网贵州分公司", "category", "电力", "credit", "A+", "validUntil", "长期"),
                Map.of("name", "贵州公路运输集团", "category", "物流承运", "credit", "B+", "validUntil", "2027-03-31")));
    }

    @PostMapping("/suppliers")
    public Result<Map<String, Object>> addSupplier(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of("saved", true, "name", body.getOrDefault("name", "")));
    }

    @PostMapping("/flows")
    public Result<Map<String, Object>> addFlow(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of("saved", true, "traceNo", "SC-" + System.currentTimeMillis() % 100000));
    }
}