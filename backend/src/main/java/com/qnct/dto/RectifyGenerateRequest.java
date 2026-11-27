package com.qnct.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 整改计划生成入参（审批退回/预警事项 → AI 措施 + 预算）
 */
public record RectifyGenerateRequest(
        @NotBlank(message = "事项编号不能为空") String issueNo,
        @NotBlank(message = "事项描述不能为空") String topic,
        String risk,
        String source) {
}