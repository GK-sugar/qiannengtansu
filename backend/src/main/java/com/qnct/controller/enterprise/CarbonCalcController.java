package com.qnct.controller.enterprise;

import com.qnct.common.Result;
import com.qnct.dto.CalcRequest;
import com.qnct.dto.CalcResultDto;
import com.qnct.service.CarbonCalcService;
import com.qnct.service.HashChainService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 企业端 · 碳排核算（F1~F5 引擎 + F10 结果上链存证）
 */
@RestController
@RequestMapping("/api/v1/enterprise/calc")
public class CarbonCalcController {

    private final CarbonCalcService calcService;
    private final HashChainService hashChain;

    public CarbonCalcController(CarbonCalcService calcService, HashChainService hashChain) {
        this.calcService = calcService;
        this.hashChain = hashChain;
    }

    @PostMapping("/run")
    public Result<CalcResultDto> run(@Valid @RequestBody CalcRequest req) {
        return Result.ok(calcService.calc(req));
    }

    /** 核算报告快照上链（F10 存证，返回链上区块） */
    @PostMapping("/seal")
    public Result<Map<String, Object>> seal(@RequestBody Map<String, Object> snapshot) {
        Map<String, Object> payload = snapshot.isEmpty() ? seedSnapshot() : snapshot;
        var block = hashChain.append("CARBON_RESULT", 202610001L, "CR-" + payload.get("period"),
                payload, 4L);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("blockSeq", block.getBlockSeq());
        out.put("dataHash", block.getDataHash());
        out.put("sealed", true);
        return Result.ok(out);
    }

    private Map<String, Object> seedSnapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("period", "2026-10");
        m.put("scope1", "2114.36 kgCO2e");
        m.put("scope2", "4808.90 kgCO2e");
        m.put("scope3", "457.39 kgCO2e");
        m.put("total", "7380.65 kgCO2e");
        return m;
    }
}