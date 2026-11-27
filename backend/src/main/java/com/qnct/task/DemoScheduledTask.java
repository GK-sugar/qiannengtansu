package com.qnct.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：Token 巡检 / 大屏快照 / 信誉分刷新（演示：每 5 分钟心跳日志）
 */
@Slf4j
@Component
public class DemoScheduledTask {

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void heartbeat() {
        log.info("[定时任务] 心跳：Token 巡检 / 缓存刷新 / 监控快照（演示模式）");
    }
}