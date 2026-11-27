package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_audit_log")
public class SysAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private Long orgId;
    private String module;
    private String action;
    private String resourceType;
    private Long resourceId;
    private String method;
    private String url;
    private String ip;
    private String userAgent;
    private String requestParams;
    private Integer responseStatus;
    private String result;
    private Integer durationMs;
    private LocalDateTime operateTime;
}