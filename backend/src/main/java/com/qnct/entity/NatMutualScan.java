package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 跨国互认监控 · 海外扫码查验记录（公开验真页 /verify/{token}）
 */
@Data
@TableName("nat_mutual_scan")
public class NatMutualScan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String passportNo;
    /** 来源国家 / 地区（欧盟成员国等） */
    private String country;
    private String city;
    private String ip;
    /** 查验结果：VERIFIED / DISCREPANCY */
    private String verifyResult;
    private LocalDateTime scanTime;
    @TableLogic
    private Integer deleted;
}