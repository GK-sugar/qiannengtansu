package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sc_supplier")
public class ScSupplier {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orgId;
    /** SUPPLIER 供应商 / CUSTOMER 客户 */
    private String partnerType;
    private String name;
    private String creditCode;
    private String contactName;
    private String contactPhone;
    private String materialScope;
    private String region;
    /** ENABLED / DISABLED */
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}