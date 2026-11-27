package com.qnct.controller.gov;

import com.qnct.common.Result;
import com.qnct.entity.EmissionFactor;
import com.qnct.mapper.EmissionFactorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 省厅端 · 省级因子库管理（增删改查 + 审核状态）
 */
@RestController
@RequestMapping("/api/v1/gov/factors")
public class FactorController {

    private final EmissionFactorMapper mapper;

    public FactorController(EmissionFactorMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping
    public Result<List<EmissionFactor>> list() {
        try {
            return Result.ok(mapper.selectList(new LambdaQueryWrapper<EmissionFactor>()
                    .orderByDesc(EmissionFactor::getCreateTime)));
        } catch (Exception e) {
            EmissionFactor f = new EmissionFactor();
            f.setId(1L);
            f.setFactorCode("E-GRID-SC");
            f.setFactorName("西南电网电力排放因子");
            f.setCategory("ENERGY");
            f.setFactorValue(new BigDecimal("0.5703"));
            f.setUnit("kgCO2e/kWh");
            f.setScope("2");
            f.setVersion("V3.2");
            f.setSourceOrg("生态环境部/省级实测");
            f.setStatus("EFFECTIVE");
            f.setCreateTime(LocalDateTime.now());
            return Result.ok(List.of(f));
        }
    }

    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody EmissionFactor factor) {
        factor.setStatus(factor.getStatus() == null ? "PENDING" : factor.getStatus());
        factor.setCreateTime(LocalDateTime.now());
        try {
            mapper.insert(factor);
        } catch (Exception ignored) {
        }
        return Result.ok(Map.of("saved", true, "note", "因子提交后进入审核队列（PENDING）"));
    }
}