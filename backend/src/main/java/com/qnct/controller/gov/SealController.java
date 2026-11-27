package com.qnct.controller.gov;

import com.qnct.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 省厅端 · 电子签章（本地 SVG 合成 + 上链）
 */
@RestController
@RequestMapping("/api/v1/gov/seal")
public class SealController {

    @PostMapping("/sign")
    public Result<Map<String, Object>> sign(@RequestBody Map<String, Object> body) {
        String docNo = String.valueOf(body.getOrDefault("docNo", "YS-2026-" + System.currentTimeMillis() % 10000));
        return Result.ok(Map.of("docNo", docNo, "signedAt", java.time.LocalDateTime.now().toString(),
                "seal", "/assets/seal/gov-seal.svg", "note", "意见书已盖贵州省生态环境厅电子签章（演示模式，二期可接 CA）"));
    }
}