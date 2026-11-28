package com.qnct.controller.gov;

import com.qnct.common.PageResult;
import com.qnct.common.Result;
import com.qnct.service.InspectionService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 省厅端 · 线下核查任务生命周期（与移动执法 H5 打卡联动）
 *   PENDING → DOING → DONE；超期 → OVERDUE；PENDING/DOING → CANCELED
 */
@RestController
@RequestMapping("/api/v1/gov/inspection")
public class InspectionController {

    private final InspectionService inspection;

    public InspectionController(InspectionService inspection) {
        this.inspection = inspection;
    }

    @GetMapping("/tasks")
    public Result<PageResult<Map<String, Object>>> tasks(@RequestParam(required = false) String status,
                                                         @RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size) {
        return Result.ok(inspection.list(status, page, size));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(inspection.detail(id));
    }

    @PostMapping("/dispatch")
    public Result<Map<String, Object>> dispatch(@RequestBody Map<String, Object> body) {
        return Result.ok(inspection.dispatch(body));
    }

    @PostMapping("/{id}/start")
    public Result<Map<String, Object>> start(@PathVariable Long id) {
        return Result.ok(inspection.start(id));
    }

    @PostMapping("/{id}/checkin")
    public Result<Map<String, Object>> checkin(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.ok(inspection.checkin(id, body));
    }

    @PostMapping("/{id}/complete")
    public Result<Map<String, Object>> complete(@PathVariable Long id,
                                                @RequestBody(required = false) Map<String, Object> body) {
        return Result.ok(inspection.complete(id, body == null ? Map.of() : body));
    }

    @PostMapping("/{id}/cancel")
    public Result<Map<String, Object>> cancel(@PathVariable Long id,
                                              @RequestBody(required = false) Map<String, Object> body) {
        String reason = body == null ? null : String.valueOf(body.getOrDefault("reason", ""));
        return Result.ok(inspection.cancel(id, reason));
    }

    @PostMapping("/{id}/archive")
    public Result<Map<String, Object>> archive(@PathVariable Long id) {
        return Result.ok(inspection.archive(id));
    }
}