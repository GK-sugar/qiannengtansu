package com.qnct.event;

import com.qnct.service.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 业务事件 → 站内通知：监听各模块状态变化，统一推送给责任人与企业。
 */
@Component
public class NotificationEventListener {

    private final NotificationService notifications;

    public NotificationEventListener(NotificationService notifications) {
        this.notifications = notifications;
    }

    @EventListener
    public void onBusinessEvent(BizNotificationEvent e) {
        notifications.send(e.userId(), e.bizType(), e.title(), e.content());
        notifications.sendToOrg(e.orgId(), e.bizType(), e.title(), e.content());
    }
}