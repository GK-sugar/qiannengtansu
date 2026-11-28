package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ent_logistics_trip")
public class EntLogisticsTrip {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tripNo;
    private Long orgId;
    private Long scFlowId;
    private String plateNo;
    private String transportMode;
    private String startName;
    private BigDecimal startLng;
    private BigDecimal startLat;
    private String endName;
    private BigDecimal endLng;
    private BigDecimal endLat;
    /** 轨迹点数组 JSON: [[lng,lat],...] */
    private String routeJson;
    private BigDecimal mileageKm;
    /** IN_TRANSIT / ARRIVED */
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime arriveTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}