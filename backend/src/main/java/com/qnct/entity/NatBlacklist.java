package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 黑名单通报系统 · 违规企业记录（标记 / 冻结权限 / 通报）
 */
@Data
@TableName("nat_blacklist")
public class NatBlacklist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String entName;
    private Long orgId;
    /** 违规原因 */
    private String reason;
    /** 严重级别：HIGH / MEDIUM */
    private String severity;
    /** 处置状态：FROZEN 已冻结 / NOTIFIED 已通报 / OBSERVING 观察中 */
    private String penaltyStatus;
    /** 证据来源：数据核验 / 海关协查 / 匿名举报 */
    private String source;
    private String evidenceNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}