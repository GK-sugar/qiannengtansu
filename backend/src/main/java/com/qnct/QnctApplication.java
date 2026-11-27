package com.qnct;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 黔能碳溯 · 后端启动类（单体应用，单机部署）
 * 启动环境（开发）： java -jar qnct-backend.jar --spring.profiles.active=dev
 * 依赖 MySQL 8.0 / Redis 作为数据与缓存底座；两个服务未启动时，
 * 各业务服务会自动降级为「演示模式」，接口照常可用（与前端 VITE_USE_MOCK 同思路）。
 */
@SpringBootApplication
@MapperScan("com.qnct.mapper")
@EnableAsync
@EnableScheduling
public class QnctApplication {

    public static void main(String[] args) {
        SpringApplication.run(QnctApplication.class, args);
    }
}