package com.qnct.controller.enterprise;

import com.qnct.common.Result;
import com.qnct.service.CreditScoreService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 企业端 · 绿色金融：F7 信誉分 + F8 预授信额度
 */
@RestController
@RequestMapping("/api/v1/enterprise/finance")
public class FinanceController {

    private final CreditScoreService credit;

    public FinanceController(CreditScoreService credit) {
        this.credit = credit;
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        return Result.ok(credit.profile(92, 88, 81, 95, 30_000_000L));
    }

    @PostMapping("/apply")
    public Result<Map<String, Object>> apply(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of("applyNo", "FI-" + System.currentTimeMillis() % 1000000,
                "status", "PENDING", "note", "预授信申请已提交银行端"));
    }
}