package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("gov_inspection_task")
public class GovInspectionTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;
    private String title;
    private Long orgId;
    /** 数据核查 / 现场执法 / 飞检 */
    private String type;
    private Long leaderId;
    private Long executorId;
    private LocalDateTime planStart;
    private LocalDateTime planEnd;
    private String address;
    private BigDecimal lng;
    private BigDecimal lat;
    private Integer radiusM;
    /** MySQL 保留字，需反引号 */
    @TableField("`desc`")
    private String desc;
    /** PENDING / DOING / DONE / OVERDUE / CANCELED */
    private String status;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}