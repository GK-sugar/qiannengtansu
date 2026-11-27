package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("emission_factor")
public class EmissionFactor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String factorCode;
    private String factorName;
    private String category;
    private BigDecimal factorValue;
    private String unit;
    private String scope;
    private String version;
    private String sourceOrg;
    private Long ownerOrgId;
    private String dataScope;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String status;
    private Long auditBy;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}