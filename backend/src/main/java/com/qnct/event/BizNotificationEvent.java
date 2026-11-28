package com.qnct.event;

/**
 * 业务事件：审批/核查/因子/整改等状态变化统一发布，由通知中心落站内信。
 * userId 为定向通知人（核查执行人/发起人），orgId 为需要触达的企业或部门机构。
 */
public record BizNotificationEvent(String bizType, Long userId, Long orgId, String title, String content) {
}