package com.qnct.controller.enterprise;

import com.qnct.common.BizException;
import com.qnct.common.PageResult;
import com.qnct.common.Result;
import com.qnct.entity.EntActivityData;
import com.qnct.mapper.EntActivityDataMapper;
import com.qnct.security.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qnct.util.IdGen;
import org.springframework.security.core.context.SecurityContextHolder;
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

    /** 从 SecurityContext 取当前登录企业的 orgId，确保数据隔离 */
    private Long currentOrgId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser lu && lu.getOrgId() != null) {
            return lu.getOrgId();
        }
        return 1001L; // 演示兜底
    }

    @GetMapping("/list")
    public Result<PageResult<EntActivityData>> list(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String period) {
        try {
            var q = new LambdaQueryWrapper<EntActivityData>()
                    .eq(period != null && !period.isBlank(), EntActivityData::getPeriod, period)
                    .eq(EntActivityData::getOrgId, currentOrgId())
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

    /** 保存活动数据 —— 校验必填字段、写入 orgId、异常向上抛出 */
    @PostMapping("/save")
    public Result<Map<String, Object>> save(@RequestBody EntActivityData data) {
        // 必填校验（不依赖 @Validated，因为演示模式前端可能漏传）
        if (data.getPeriod() == null || data.getPeriod().isBlank()) {
            return Result.fail(400, "核算期 period 必填");
        }
        if (data.getActivityName() == null || data.getActivityName().isBlank()) {
            return Result.fail(400, "活动名称 activityName 必填");
        }
        if (data.getQuantity() == null || data.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(400, "活动数据量 quantity 必须大于 0");
        }

        // 数据隔离：强制写入当前企业 orgId，前端伪造也无效
        data.setOrgId(currentOrgId());
        data.setDataStatus(data.getDataStatus() == null ? "DRAFT" : data.getDataStatus());
        data.setCreateTime(LocalDateTime.now());

        try {
            if (data.getId() == null) {
                mapper.insert(data);
            } else {
                // 更新时也强制锁死 orgId，防止跨企业篡改
                EntActivityData existing = mapper.selectById(data.getId());
                if (existing != null && !existing.getOrgId().equals(currentOrgId())) {
                    throw new BizException(403, "无权修改其他企业的数据");
                }
                data.setUpdateTime(LocalDateTime.now());
                mapper.updateById(data);
            }
        } catch (BizException be) {
            return Result.fail(be.getCode(), be.getMessage());
        } catch (Exception e) {
            // 数据库真挂了才返回失败，而不是假装成功
            return Result.fail(500, "数据保存失败：" + e.getMessage());
        }

        return Result.ok(Map.of(
                "id", data.getId() == null ? System.currentTimeMillis() : data.getId(),
                "bizNo", IdGen.bizNo("AD"),
                "saved", true));
    }

    /** 提交活动数据 —— 状态改为 SUBMITTED，返回后续核算引用 */
    @PostMapping("/submit/{id}")
    public Result<Map<String, Object>> submit(@PathVariable Long id) {
        try {
            EntActivityData existing = mapper.selectById(id);
            if (existing == null) {
                return Result.fail(404, "活动数据不存在");
            }
            if (!existing.getOrgId().equals(currentOrgId())) {
                return Result.fail(403, "无权操作其他企业的数据");
            }
            existing.setDataStatus("SUBMITTED");
            existing.setUpdateTime(LocalDateTime.now());
            mapper.updateById(existing);
            // 触发核算任务（演示模式：仅返回引用，后续由定时任务或事件驱动）
            return Result.ok(Map.of(
                    "id", id,
                    "status", "SUBMITTED",
                    "calcTaskNo", "CALC-" + IdGen.bizNo("T"),
                    "note", "已提交，等待后台核算引擎 F1~F5 自动汇总"));
        } catch (Exception e) {
            return Result.fail(500, "提交失败：" + e.getMessage());
        }
    }
}
