package com.qnct.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 核算结果：范围一二三分量 + 总量 + 碳强度（F1~F5）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalcResultDto {
    private String period;
    private BigDecimal scope1;
    private BigDecimal scope2;
    private BigDecimal scope3;
    private BigDecimal total;
    private BigDecimal intensity;
    private String intensityUnit;
    private List<CalcItemResult> items;
}