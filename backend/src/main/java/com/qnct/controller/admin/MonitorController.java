package com.qnct.controller.admin;

import com.qnct.common.Result;
import com.qnct.service.MetricsService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 超管端 · 系统运行与接口监控：服务器负载 / API 调用频率 / 在线会话
 */
@RestController
@RequestMapping("/api/v1/admin/monitor")
public class MonitorController {

    private final MetricsService metrics;
    private final AtomicInteger online = new AtomicInteger(286);

    public MonitorController(MetricsService metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/metrics")
    public Result<Map<String, Object>> metrics() {
        Map<String, Object> m = new LinkedHashMap<>(metrics.snapshot());
        m.put("onlineUsers", online.get() + (int) (Math.random() * 6));
        m.put("collectedAt", java.time.LocalDateTime.now().toString());
        return Result.ok(m);
    }

    @GetMapping("/history")
    public Result<Map<String, Object>> history() {
        List<String> times = new ArrayList<>();
        List<Double> cpu = new ArrayList<>();
        List<Double> mem = new ArrayList<>();
        List<Integer> qps = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            times.add(i * 30 + "s前");
            cpu.add(Math.round((32 + Math.sin(i / 3.0) * 9 + Math.random() * 6) * 10) / 10.0);
            mem.add(Math.round((55 + Math.cos(i / 4.0) * 6 + Math.random() * 4) * 10) / 10.0);
            qps.add((int) (60 + Math.sin(i / 4.0) * 28 + Math.random() * 22));
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("times", times);
        m.put("cpu", cpu);
        m.put("mem", mem);
        m.put("qps", qps);
        return Result.ok(m);
    }

    @GetMapping("/apis")
    public Result<List<Map<String, Object>>> apis() {
        return Result.ok(List.of(
                Map.of("path", "/api/v1/enterprise/ai/chat", "name", "AI 碳管家", "source", "LLM", "calls", 3124, "avgMs", 860, "err", "0.4%"),
                Map.of("path", "/api/v1/enterprise/calc/run", "name", "碳排核算", "source", "内部", "calls", 2410, "avgMs", 32, "err", "0.0%"),
                Map.of("path", "/api/v1/gov/screen/summary", "name", "大屏快照", "source", "内部", "calls", 1866, "avgMs", 18, "err", "0.0%"),
                Map.of("path", "/api/v1/enterprise/lab-report/recognize", "name", "化验单识别", "source", "VISION", "calls", 1204, "avgMs", 1240, "err", "1.2%"),
                Map.of("path", "/api/v1/public/verify", "name", "扫码验真", "source", "内部", "calls", 923, "avgMs", 26, "err", "0.0%")));
    }

    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> sessions() {
        return Result.ok(List.of(
                Map.of("user", "系统管理员", "role", "超级管理员", "ip", "10.20.1.9", "loginAt", "08:52", "active", "3min前"),
                Map.of("user", "李处长", "role", "省厅审批员", "ip", "10.10.3.21", "loginAt", "09:10", "active", "1min前"),
                Map.of("user", "陈主任", "role", "核查复核员", "ip", "172.16.8.7", "loginAt", "09:47", "active", "12min前"),
                Map.of("user", "王总", "role", "企业管理员", "ip", "223.104.19.6", "loginAt", "09:03", "active", "刚刚")));
    }

    /** 演示：模拟在线人数波动 */
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh() {
        int n = online.updateAndGet(v -> Math.max(100, v + (int) (Math.random() * 20 - 10)));
        return Result.ok(Map.of("onlineUsers", n));
    }
}