package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 国家级审批部门 · 终审与发证记录（碳足迹护照终审签发）
 */
@Data
@TableName("nat_final_issue")
public class NatFinalIssue {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 申请单号（关联 passport_application.no） */
    private String applicationNo;
    /** 护照编号（终审通过签发后生成） */
    private String passportNo;
    private Long orgId;
    private String entName;
    private String period;
    private String productName;
    /** 核查机构复核结论：PASSED / REJECTED */
    private String agencyConclusion;
    /** 终审状态：PENDING / APPROVED / REJECTED */
    private String status;
    private BigDecimal totalCo2e;
    private BigDecimal intensity;
    /** F10 哈希链区块号（防篡改存证） */
    private Long hashChainSeq;
    /** F11 防伪验真令牌（二维码内容） */
    private String verifyToken;
    private String signerName;
    private LocalDateTime issueTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}