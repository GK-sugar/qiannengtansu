package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 国际关税预警 · CBAM 关税规则（规则推演与影响测算）
 */
@Data
@TableName("nat_tariff_rule")
public class NatTariffRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 行业：钢铁 / 铝 / 水泥 / 电力 / 氢 / 化肥 */
    private String industry;
    /** 产品范围描述 */
    private String productScope;
    /** CBAM 碳关税税率（EUR/tCO2e） */
    private BigDecimal tariffRate;
    /** 免费配额比例（%） */
    private BigDecimal freeAllowance;
    /** 生效日期 */
    private LocalDate effectiveFrom;
    /** 影响测算口径说明 */
    private String note;
    /** ACTIVE / FROZEN */
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}