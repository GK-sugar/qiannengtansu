package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fin_credit_profile")
public class FinCreditProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orgId;
    private BigDecimal score;
    private String level;
    /** 评分明细 JSON：完整性/及时性/稳定性/合规 */
    private String scoreDetailJson;
    /** 基准额度（万元） */
    private BigDecimal baseQuota;
    /** 预授信额度（万元） */
    private BigDecimal creditQuota;
    private Integer historyCount;
    private Integer applyCount;
    private LocalDateTime calcTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}