package com.qnct.service;

import com.qnct.common.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 初审波动预警规则引擎（F6）：
 *   波动率 ρ = |本期 − 基期| / 基期；ρ > 阈值(默认 20%) 判异常并给出方向与原因。
 */
@Service
public class RuleEngineService {

    public List<Anomaly> check(List<Indicator> indicators) {
        return check(indicators, Constants.DEFAULT_FLUCTUATION_THRESHOLD);
    }

    public List<Anomaly> check(List<Indicator> indicators, double threshold) {
        List<Anomaly> out = new ArrayList<>();
        for (Indicator ind : indicators) {
            BigDecimal base = ind.base();
            if (base == null || base.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal rho = ind.current().subtract(base).abs()
                    .divide(base, 6, RoundingMode.HALF_UP);
            if (rho.doubleValue() > threshold) {
                String direction = ind.current().compareTo(base) > 0 ? "偏高" : "偏低";
                out.add(new Anomaly(ind.name(), ind.current(), base,
                        rho.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP),
                        direction,
                        ind.name() + "环比波动 " + rho.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                                + "%(" + direction + ")，超过 ±" + BigDecimal.valueOf(threshold * 100).stripTrailingZeros().toPlainString() + "% 阈值"));
            }
        }
        return out;
    }

    public record Indicator(String name, BigDecimal current, BigDecimal base) {}

    @Data
    @AllArgsConstructor
    public static class Anomaly {
        private String name;
        private BigDecimal current;
        private BigDecimal base;
        private BigDecimal rho;
        private String direction;
        private String desc;
    }
}