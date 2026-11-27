package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ent_rectify_budget")
public class EntRectifyBudget {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long issueId;
    /** 费用科目：设备改造/检测费用/人员培训/第三方服务 等 */
    private String cat;
    private String basis;
    private BigDecimal qty;
    private String unit;
    private BigDecimal price;
    private BigDecimal amount;
    private LocalDateTime createTime;
}