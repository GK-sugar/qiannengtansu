package com.qnct.controller.enterprise;

import com.qnct.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 企业端 · 护照申请与状态追踪（状态机：填报→省审→复核→发证）
 */
@RestController
@RequestMapping("/api/v1/enterprise/passport")
public class PassportController {

    @GetMapping("/applications")
    public Result<List<Map<String, Object>>> applications() {
        return Result.ok(List.of(
                Map.of("no", "SQ-2026-0018", "product", "精煤（洗选产品）", "standard", "ISO 14067",
                        "status", "ISSUED", "progress", 100, "applyTime", "2026-09-12"),
                Map.of("no", "SQ-2026-0041", "product", "磷酸铁锂正极材料", "standard", "EU Battery",
                        "status", "PENDING_AGENCY", "progress", 62, "applyTime", "2026-10-28"),
                Map.of("no", "SQ-2026-0056", "product", "精煤 Q3 核证", "standard", "CBAM",
                        "status", "PENDING_GOV", "progress", 30, "applyTime", "2026-11-18")));
    }

    @PostMapping("/apply")
    public Result<Map<String, Object>> apply(@RequestBody Map<String, String> body) {
        return Result.ok(Map.of("no", "SQ-2026-" + (System.currentTimeMillis() % 9000 + 1000),
                "status", "DRAFT", "step", "草稿已保存，可补充报告后提交省审"));
    }
}