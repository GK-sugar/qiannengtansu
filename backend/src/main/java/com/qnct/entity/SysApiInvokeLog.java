package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_api_invoke_log")
public class SysApiInvokeLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String apiType;
    private String bizType;
    private Long orgId;
    private Long userId;
    private String requestSummary;
    private Integer success;
    private Integer costTokens;
    private Integer durationMs;
    private String errorMsg;
    private LocalDateTime createTime;
}