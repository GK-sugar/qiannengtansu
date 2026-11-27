package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("gov_anomaly_alert")
public class GovAnomalyAlert {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String alertNo;
    private Long orgId;
    private String dataType;
    private Long dataId;
    private String ruleCode;
    private String alertLevel;
    private String alertDesc;
    private String compareDesc;
    private String status;
    private Long handlerId;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;
}