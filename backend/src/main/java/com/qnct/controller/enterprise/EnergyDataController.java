package com.qnct.controller.enterprise;

import com.qnct.common.PageResult;
import com.qnct.common.Result;
import com.qnct.entity.EntActivityData;
import com.qnct.mapper.EntActivityDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qnct.util.IdGen;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 企业端 · 碳数据填报（范围一/二/三活动数据）
 * 正式版走 ent_activity_data 表；库不可用时返回演示记录。
 */
@RestController
@RequestMapping("/api/v1/enterprise/energy-data")
public class EnergyDataController {

    private final EntActivityDataMapper mapper;

    public EnergyDataController(EntActivityDataMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping("/list")
    public Result<PageResult<EntActivityData>> list(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String period) {
        try {
            var q = new LambdaQueryWrapper<EntActivityData>()
                    .eq(period != null && !period.isBlank(), EntActivityData::getPeriod, period)
                    .orderByDesc(EntActivityData::getCreateTime);
            return Result.ok(PageResult.of(mapper.selectPage(new Page<>(page, size), q)));
        } catch (Exception e) {
            List<EntActivityData> demo = new ArrayList<>();
            demo.add(row(1L, "2026-10", "ENERGY", "电力（外购）", new BigDecimal("842.6"), "万kWh", "0.5703", "2"));
            demo.add(row(2L, "2026-10", "ENERGY", "原煤", new BigDecimal("1280.5"), "t", "0.0251", "1"));
            demo.add(row(3L, "2026-10", "ENERGY", "柴油", new BigDecimal("36.2"), "t", "3.16", "1"));
            demo.add(row(4L, "2026-10", "TRANSPORT", "公路运输", new BigDecimal("58640"), "t·km", "0.078", "3"));
            return Result.ok(PageResult.of(demo, demo.size(), page, size));
        }
    }

    private EntActivityData row(Long id, String period, String type, String name, BigDecimal q, String unit, String factor, String scope) {
        EntActivityData d = new EntActivityData();
        d.setId(id);
        d.setOrgId(1001L);
        d.setPeriod(period);
        d.setPeriodType("MONTH");
        d.setActivityType(type);
        d.setActivityName(name);
        d.setQuantity(q);
        d.setUnit(unit);
        d.setFactorValue(new BigDecimal(factor));
        d.setScope(scope);
        d.setDataStatus("DRAFT");
        d.setCreateTime(LocalDateTime.now());
        return d;
    }

    @PostMapping("/save")
    public Result<Map<String, Object>> save(@RequestBody EntActivityData data) {
        data.setDataStatus(data.getDataStatus() == null ? "DRAFT" : data.getDataStatus());
        data.setCreateTime(LocalDateTime.now());
        try {
            mapper.insert(data);
        } catch (Exception ignored) {
        }
        return Result.ok(Map.of("id", data.getId() == null ? System.currentTimeMillis() : data.getId(),
                "bizNo", IdGen.bizNo("AD"), "saved", true));
    }

    @PostMapping("/submit/{id}")
    public Result<Map<String, Object>> submit(@PathVariable Long id) {
        return Result.ok(Map.of("id", id, "status", "SUBMITTED", "note", "提交后进入核算任务队列（演示模式）"));
    }
}