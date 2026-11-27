package com.qnct.controller.agency;

import com.qnct.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 核查机构端 · 企业数据包接收
 */
@RestController
@RequestMapping("/api/v1/agency/packages")
public class AgencyPackageController {

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(List.of(
                Map.of("no", "CBAM-2026-10", "org", "贵州黔能矿业", "period", "2026-10", "rows", 1280,
                        "receivedAt", "2026-11-21", "status", "待复核"),
                Map.of("no", "EU-BAT-2026-09", "org", "贵州中伟新材料", "period", "2026-09", "rows", 1450,
                        "receivedAt", "2026-11-18", "status", "复核中")));
    }

    @PostMapping("/{no}/accept")
    public Result<Map<String, Object>> accept(@PathVariable String no) {
        return Result.ok(Map.of("no", no, "accepted", true));
    }
}