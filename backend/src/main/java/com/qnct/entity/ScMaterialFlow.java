package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sc_material_flow")
public class ScMaterialFlow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orgId;
    private Long partnerId;
    /** IN 采购 / OUT 销售 */
    private String flowType;
    private String materialName;
    private BigDecimal quantity;
    private String unit;
    private String period;
    private String transportMode;
    private BigDecimal transportDistance;
    private BigDecimal allocatedEmission;
    /** DRAFT / CONFIRMED */
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}