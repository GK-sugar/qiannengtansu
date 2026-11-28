package com.qnct.controller;

import com.qnct.common.PageResult;
import com.qnct.common.Result;
import com.qnct.entity.SysNotification;
import com.qnct.security.SecurityUtils;
import com.qnct.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 站内通知中心：列表 / 未读数 / 已读 / 全部已读（四端通用）
 */
@RestController
@RequestMapping("/api/v1/notify")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping("/list")
    public Result<PageResult<SysNotification>> list(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return Result.ok(notifications.list(SecurityUtils.userId(4L), page, size, unreadOnly));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount() {
        long n = notifications.unreadCount(SecurityUtils.userId(4L));
        return Result.ok(Map.of("count", n));
    }

    @PostMapping("/{id}/read")
    public Result<Map<String, Object>> read(@PathVariable Long id) {
        boolean ok = notifications.markRead(id, SecurityUtils.userId(4L));
        return Result.ok(Map.of("read", ok));
    }

    @PostMapping("/read-all")
    public Result<Map<String, Object>> readAll() {
        return Result.ok(Map.of("updated", notifications.markAllRead(SecurityUtils.userId(4L))));
    }
}