package com.qnct.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 单条活动数据核算结果（叶子节点）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalcItemResult {
    private String activityName;
    private String activityType;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal factorValue;
    private String scope;
    private BigDecimal emission;
    private String formula;
}