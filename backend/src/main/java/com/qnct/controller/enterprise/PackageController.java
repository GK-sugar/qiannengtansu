package com.qnct.controller.enterprise;

import com.qnct.common.Result;
import com.qnct.service.HashChainService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业端 · 出海合规数据包（CBAM 模板拼装 + 上链存证）
 */
@RestController
@RequestMapping("/api/v1/enterprise/package")
public class PackageController {

    private final HashChainService hashChain;

    public PackageController(HashChainService hashChain) {
        this.hashChain = hashChain;
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(List.of(
                Map.of("no", "CBAM-2026-10", "period", "2026-10", "rows", 1280, "size", "1.2MB", "status", "READY"),
                Map.of("no", "CBAM-2026-09", "period", "2026-09", "rows", 1194, "size", "1.1MB", "status", "SEALED")));
    }

    /** 导出数据包（Excel 组装）+ 哈希存证 */
    @PostMapping("/export")
    public Result<Map<String, Object>> export(@RequestBody Map<String, Object> req) {
        String no = "CBAM-" + req.getOrDefault("period", "2026-10");
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("packageNo", no);
        snap.put("rows", 1280);
        snap.put("standard", "CBAM-TRANSITION");
        var block = hashChain.append("PACKAGE", 202610001L, no, snap, 4L);
        return Result.ok(Map.of("file", "/files/package/" + no + ".xlsx",
                "blockSeq", block.getBlockSeq(), "hash", block.getDataHash()));
    }
}