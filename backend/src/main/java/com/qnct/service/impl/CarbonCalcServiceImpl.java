package com.qnct.service.impl;

import com.qnct.dto.CalcItem;
import com.qnct.dto.CalcItemResult;
import com.qnct.dto.CalcRequest;
import com.qnct.dto.CalcResultDto;
import com.qnct.service.CarbonCalcService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * F1~F5 核算引擎实现（纯函数，便于单测与审计复算）
 */
@Service
public class CarbonCalcServiceImpl implements CarbonCalcService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Override
    public CalcResultDto calc(CalcRequest req) {
        BigDecimal s1 = ZERO, s2 = ZERO, s3 = ZERO;
        List<CalcItemResult> items = new ArrayList<>();
        for (CalcItem it : req.items()) {
            BigDecimal factor = it.factorValue() == null ? ZERO : it.factorValue();
            BigDecimal emission = it.quantity().multiply(factor).setScale(4, RoundingMode.HALF_UP);
            String scope = it.scope() == null ? "1" : it.scope();
            String formula = "E = AD × EF = " + it.quantity().stripTrailingZeros().toPlainString()
                    + " × " + factor.stripTrailingZeros().toPlainString();
            items.add(new CalcItemResult(it.activityName(), it.activityType(), it.quantity(),
                    it.unit(), factor, scope, emission, formula));
            if ("1".equals(scope)) {
                s1 = s1.add(emission);
            } else if ("2".equals(scope)) {
                s2 = s2.add(emission);
            } else {
                s3 = s3.add(emission);
            }
        }
        BigDecimal total = s1.add(s2).add(s3).setScale(4, RoundingMode.HALF_UP);
        BigDecimal intensity = (req.output() == null || req.output().compareTo(ZERO) <= 0)
                ? ZERO : total.divide(req.output(), 6, RoundingMode.HALF_UP);
        return new CalcResultDto(req.period(), s1.setScale(4, RoundingMode.HALF_UP),
                s2.setScale(4, RoundingMode.HALF_UP), s3.setScale(4, RoundingMode.HALF_UP),
                total, intensity, "kgCO2e/t" + (req.outputUnit() == null ? "" : req.outputUnit()), items);
    }
}