package com.qnct.controller;

import com.qnct.common.Result;
import com.qnct.service.PassportService;
import com.qnct.service.impl.HashChainServiceImpl;
import com.qnct.service.RuleEngineService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公开接口（免登录）：
 *  /verify/{token} 扫码验真页：护照信息 + 存证哈希 + 篡改判定（F10/F11）
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicController {

    private final PassportService passportService;
    private final HashChainServiceImpl hashChain;
    private final RuleEngineService ruleEngine;

    public PublicController(PassportService passportService, HashChainServiceImpl hashChain,
                            RuleEngineService ruleEngine) {
        this.passportService = passportService;
        this.hashChain = hashChain;
        this.ruleEngine = ruleEngine;
    }

    @GetMapping("/verify/{token}")
    public Result<Map<String, Object>> verify(@PathVariable String token) {
        return Result.ok(passportService.verify(token));
    }

    /** 公开公式示例（供移动端展示 F1~F6） */
    @GetMapping("/formulas")
    public Result<List<Map<String, String>>> formulas() {
        return Result.ok(List.of(
                Map.of("no", "F1", "name", "范围一直排", "formula", "E = AD × NCV × EF × OF"),
                Map.of("no", "F2", "name", "范围二购电", "formula", "E = AD × EF电网"),
                Map.of("no", "F3", "name", "范围三运输", "formula", "E = Σ(货重 × 里程 × EF)"),
                Map.of("no", "F6", "name", "波动预警", "formula", "ρ = |本期 − 基期| / 基期 ≥ 20%"),
                Map.of("no", "F9", "name", "Haversine 判距", "formula", "d = 2R·asin(√(sin²Δφ/2 + cosφ1cosφ2sin²Δλ/2))"),
                Map.of("no", "F10", "name", "哈希链存证", "formula", "h(i) = SHA256(salt ‖ h(i−1) ‖ SHA256(datai ‖ salt))")));
    }

    /** 演示：F6 波动判定在线计算器（公开页辅助） */
    @GetMapping("/anomaly-demo")
    public Result<Map<String, Object>> anomalyDemo(
            @RequestParam(defaultValue = "841.6") BigDecimal current,
            @RequestParam(defaultValue = "657.0") BigDecimal base) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("current", current);
        m.put("base", base);
        m.put("anomalies", ruleEngine.check(List.of(
                new RuleEngineService.Indicator("外购电力", current, base))));
        return Result.ok(m);
    }
}