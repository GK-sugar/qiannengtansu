package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知（审批退回/核查派发/因子生效/互认异常等事件统一落此表）
 */
@Data
@TableName("sys_notification")
public class SysNotification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** REVIEW/INSPECTION/PASSPORT/PACKAGE/FACTOR/RECTIFY/FINANCE */
    private String bizType;
    private String title;
    private String content;
    private Integer readFlag;
    private LocalDateTime readTime;
    private LocalDateTime createTime;
}