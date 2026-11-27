package com.qnct.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CalcItem(
        @NotBlank String activityType,
        @NotBlank String activityName,
        @NotNull BigDecimal quantity,
        String unit,
        BigDecimal factorValue,
        String scope) {
}