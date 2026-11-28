package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("carbon_report")
public class CarbonReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String reportNo;
    private Long orgId;
    private Long taskId;
    private Long productId;
    private String reportName;
    private String reportType;
    private String period;
    private BigDecimal totalCo2e;
    private BigDecimal intensity;
    private String echartsConfig;
    private Long reportFileId;
    private String status;
    private String hashValue;
    private LocalDateTime generateTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    @TableLogic
    private Integer deleted;
}