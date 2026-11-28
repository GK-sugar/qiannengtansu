package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fin_credit_apply")
public class FinCreditApply {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String applyNo;
    private Long orgId;
    private String bankName;
    /** 申请额度（万元） */
    private BigDecimal applyQuota;
    private String purpose;
    private String remark;
    /** SUBMITTED / APPROVING / APPROVED / REJECTED */
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}