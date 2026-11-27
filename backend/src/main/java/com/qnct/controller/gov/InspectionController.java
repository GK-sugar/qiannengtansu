package com.qnct.controller.gov;

import com.qnct.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 省厅端 · 线下核查任务派发（与移动执法 H5 打卡联动）
 */
@RestController
@RequestMapping("/api/v1/gov/inspection")
public class InspectionController {

    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> tasks() {
        return Result.ok(List.of(
                Map.of("taskId", "T-2026-118", "org", "贵州黔能矿业", "inspector", "刘队", "type", "数据核查",
                        "range", "1.5km", "status", "进行中", "deadline", "2026-12-02"),
                Map.of("taskId", "T-2026-114", "org", "遵义铝业", "inspector", "王队", "type", "现场核查",
                        "range", "2.0km", "status", "待打卡", "deadline", "2026-12-05"),
                Map.of("taskId", "T-2026-109", "org", "贵州磷化", "inspector", "张队", "type", "复核检查",
                        "range", "0.8km", "status", "已完成", "deadline", "2026-11-25")));
    }

    @PostMapping("/dispatch")
    public Result<Map<String, Object>> dispatch(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of("taskId", "T-2026-" + (System.currentTimeMillis() % 900 + 100),
                "status", "DISPATCHED", "note", "已生成核查任务（短信通知可选，API-6）"));
    }
}