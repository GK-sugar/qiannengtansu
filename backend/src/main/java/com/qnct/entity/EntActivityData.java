package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ent_activity_data")
public class EntActivityData {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orgId;
    private Long productId;
    private Long facilityId;
    private String periodType;
    private String period;
    private String activityType;
    private String activityName;
    private BigDecimal quantity;
    private String unit;
    private String dataSource;
    private String sourceRef;
    private Long factorId;
    private BigDecimal factorValue;
    private BigDecimal gwp;
    private BigDecimal emissionCo2e;
    private String scope;
    private String dataStatus;
    private LocalDateTime submitTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    @TableLogic
    private Integer deleted;
}