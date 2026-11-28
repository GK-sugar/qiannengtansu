package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("gov_checkin_record")
public class GovCheckinRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long userId;
    private LocalDateTime checkinTime;
    private BigDecimal lng;
    private BigDecimal lat;
    private BigDecimal accuracy;
    private String address;
    private BigDecimal distanceM;
    private Long photoFileId;
    private String deviceInfo;
    /** VALID / OFFSITE / ABNORMAL */
    private String status;
    private LocalDateTime createTime;

    /** 本地存储照片的访问路径（photo_file_id 关联 sys_file 尚未接入，先随响应返回） */
    @TableField(exist = false)
    private String photoPath;
}