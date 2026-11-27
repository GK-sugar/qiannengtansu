package com.qnct.controller.agency;

import com.qnct.common.Result;
import com.qnct.service.LlmGateway;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 核查机构端 · 国际标准复核（方法学/因子/边界核对）
 */
@RestController
@RequestMapping("/api/v1/agency/review")
public class AgencyReviewController {

    private final LlmGateway llm;

    public AgencyReviewController(LlmGateway llm) {
        this.llm = llm;
    }

    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> tasks() {
        return Result.ok(List.of(
                Map.of("no", "AR-2026-031", "org", "贵州黔能矿业", "standard", "ISO 14067",
                        "checks", List.of("方法学 ✓", "因子库 ✓", "边界 ✓", "数据链 ⚠"), "status", "PENDING"),
                Map.of("no", "AR-2026-029", "org", "贵州磷化集团", "standard", "CBAM 过渡期",
                        "checks", List.of("方法学 ✓", "因子库 ✓", "边界 ✓", "数据链 ✓"), "status", "PASSED")));
    }

    /** 复核意见润色（LLM，无 Key 降级模板） */
    @PostMapping("/polish")
    public Result<Map<String, Object>> polish(@RequestBody Map<String, String> body) {
        String opinion = llm.chat("润色复核意见（专业、简短）：" + body.getOrDefault("draft", ""), null, null);
        return Result.ok(Map.of("opinion", opinion));
    }

    @PostMapping("/{no}/decide")
    public Result<Map<String, Object>> decide(@PathVariable String no, @RequestBody Map<String, String> body) {
        return Result.ok(Map.of("no", no, "decision", body.getOrDefault("decision", "PASS"),
                "comment", body.getOrDefault("comment", "")));
    }
}