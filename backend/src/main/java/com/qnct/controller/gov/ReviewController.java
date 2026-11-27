package com.qnct.controller.gov;

import com.qnct.common.Result;
import com.qnct.service.LlmGateway;
import com.qnct.service.RuleEngineService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 省厅端 · 智能数据初审台（F6 波动比对 + LLM 生成预警原因）
 */
@RestController
@RequestMapping("/api/v1/gov/review")
public class ReviewController {

    private final RuleEngineService ruleEngine;
    private final LlmGateway llm;

    public ReviewController(RuleEngineService ruleEngine, LlmGateway llm) {
        this.ruleEngine = ruleEngine;
        this.llm = llm;
    }

    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> tasks() {
        return Result.ok(List.of(
                Map.of("no", "CS-2026-0421", "org", "贵州黔能矿业", "period", "2026-10", "score", 96.2,
                        "status", "PENDING", "flag", "购电环比 +27.4%"),
                Map.of("no", "CS-2026-0419", "org", "遵义铝业", "period", "2026-10", "score", 91.4,
                        "status", "PENDING", "flag", "范围一环比 +18.6%"),
                Map.of("no", "CS-2026-0417", "org", "贵州磷化集团", "period", "2026-10", "score", 98.8,
                        "status", "PASSED", "flag", "")));
    }

    /** F6 波动判定：批量指标对比阈值 */
    @PostMapping("/check")
    public Result<List<RuleEngineService.Anomaly>> check(@RequestBody List<RuleEngineService.Indicator> indicators) {
        return Result.ok(ruleEngine.check(indicators));
    }

    /** LLM 生成预警原因（无 Key 降级知识库） */
    @PostMapping("/reason")
    public Result<Map<String, Object>> reason(@RequestBody Map<String, String> body) {
        String desc = body.getOrDefault("desc", "");
        String answer = llm.chat("请用一段话解释本省企业数据异常的潜在原因与整改建议：" + desc, null, "省厅监管端");
        return Result.ok(Map.of("reason", answer, "live", llm.live()));
    }

    @PostMapping("/{no}/pass")
    public Result<Map<String, Object>> pass(@PathVariable String no, @RequestBody Map<String, String> body) {
        return Result.ok(Map.of("no", no, "status", "PASSED", "comment", body.getOrDefault("comment", "初审通过")));
    }
}