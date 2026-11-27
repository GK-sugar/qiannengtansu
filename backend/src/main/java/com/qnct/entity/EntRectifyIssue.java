package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 整改事项（企业端「整改计划生成与预算预估」模块，见 sql/init.sql 末尾两表）
 */
@Data
@TableName("ent_rectify_issue")
public class EntRectifyIssue {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String issueNo;
    private Long orgId;
    /** 来源：初审退回 / 波动预警 / 核查退回 */
    private String sourceType;
    private String topic;
    private String riskLevel;
    /** 状态：待整改 / 整改中 / 已闭环 */
    private String status;
    /** AI 生成的整改措施清单（JSON 数组） */
    private String planJson;
    /** 预算预估明细（JSON 数组，含 10% 预备费汇总） */
    private String budgetJson;
    /** 关联哈希链区块号（防篡改存证） */
    private Long hashChainSeq;
    private LocalDateTime submitTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}