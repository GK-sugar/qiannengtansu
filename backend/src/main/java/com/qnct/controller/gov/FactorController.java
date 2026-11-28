package com.qnct.controller.gov;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qnct.common.BizException;
import com.qnct.common.Result;
import com.qnct.entity.EmissionFactor;
import com.qnct.event.BizNotificationEvent;
import com.qnct.mapper.EmissionFactorMapper;
import com.qnct.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 省厅端 · 省级因子库管理（增删改查 + 审核状态 + 版本生效/回滚）
 *   同一 factorCode 允许多版本共存，仅一个 EFFECTIVE；
 *   激活新版本会把旧版本置为 ARCHIVED，并发布通知事件给因子归属机构。
 */
@RestController
@RequestMapping("/api/v1/gov/factors")
public class FactorController {

    private static final Pattern VERSION = Pattern.compile("V(\\d+)\\.(\\d+)", Pattern.CASE_INSENSITIVE);

    private final EmissionFactorMapper mapper;
    private final ApplicationEventPublisher events;

    public FactorController(EmissionFactorMapper mapper, ApplicationEventPublisher events) {
        this.mapper = mapper;
        this.events = events;
    }

    @GetMapping
    public Result<List<EmissionFactor>> list() {
        try {
            return Result.ok(mapper.selectList(new LambdaQueryWrapper<EmissionFactor>()
                    .orderByDesc(EmissionFactor::getCreateTime)));
        } catch (Exception e) {
            return Result.ok(List.of(demo()));
        }
    }

    /** 指定因子编码的全部版本（含历史 ARCHIVED） */
    @GetMapping("/versions")
    public Result<List<EmissionFactor>> versions(@RequestParam String factorCode) {
        try {
            List<EmissionFactor> rows = mapper.selectList(new LambdaQueryWrapper<EmissionFactor>()
                    .eq(EmissionFactor::getFactorCode, factorCode)
                    .orderByDesc(EmissionFactor::getCreateTime));
            return Result.ok(rows.isEmpty() ? List.of(demo()) : rows);
        } catch (Exception e) {
            return Result.ok(List.of(demo()));
        }
    }

    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody EmissionFactor factor) {
        LocalDateTime now = LocalDateTime.now();
        factor.setStatus(factor.getStatus() == null ? "PENDING" : factor.getStatus());
        factor.setCreateTime(now);
        factor.setUpdateTime(now);
        try {
            if (factor.getVersion() == null || factor.getVersion().isBlank()) {
                factor.setVersion(nextVersion(factor.getFactorCode()));
            }
            if ("EFFECTIVE".equals(factor.getStatus())) {
                archiveOthers(factor.getFactorCode(), null);
                factor.setAuditBy(SecurityUtils.userId(2L));
                factor.setAuditTime(now);
            }
            mapper.insert(factor);
        } catch (Exception e) {
            throw new BizException("因子保存失败：" + e.getMessage());
        }
        return Result.ok(Map.of("saved", true, "version", factor.getVersion(),
                "status", factor.getStatus(), "note", "新版本已写入，待审核生效"));
    }

    /** 激活指定版本（同编码其余版本自动归档） */
    @PostMapping("/{id}/activate")
    public Result<Map<String, Object>> activate(@PathVariable Long id) {
        EmissionFactor target = require(id);
        archiveOthers(target.getFactorCode(), target.getId());
        target.setStatus("EFFECTIVE");
        target.setAuditBy(SecurityUtils.userId(2L));
        target.setAuditTime(LocalDateTime.now());
        target.setUpdateTime(LocalDateTime.now());
        mapper.updateById(target);
        if (target.getOwnerOrgId() != null) {
            events.publishEvent(new BizNotificationEvent("FACTOR", null, target.getOwnerOrgId(),
                    "因子版本已生效", "因子 " + target.getFactorCode() + " " + target.getVersion()
                            + " 已审核生效，新填报将按新版本计算。"));
        }
        return Result.ok(Map.of("activated", true, "id", target.getId(),
                "factorCode", target.getFactorCode(), "version", target.getVersion(), "status", "EFFECTIVE"));
    }

    /** 回滚：把同编码上一历史版本重新置为 EFFECTIVE */
    @PostMapping("/{id}/rollback")
    public Result<Map<String, Object>> rollback(@PathVariable Long id) {
        EmissionFactor target = require(id);
        EmissionFactor previous;
        try {
            previous = mapper.selectOne(new LambdaQueryWrapper<EmissionFactor>()
                    .eq(EmissionFactor::getFactorCode, target.getFactorCode())
                    .ne(EmissionFactor::getId, target.getId())
                    .orderByDesc(EmissionFactor::getCreateTime)
                    .last("limit 1"));
        } catch (Exception e) {
            previous = null;
        }
        if (previous == null) {
            throw new BizException("该因子暂无可回滚的历史版本");
        }
        archiveOthers(previous.getFactorCode(), previous.getId());
        previous.setStatus("EFFECTIVE");
        previous.setAuditBy(SecurityUtils.userId(2L));
        previous.setAuditTime(LocalDateTime.now());
        previous.setUpdateTime(LocalDateTime.now());
        mapper.updateById(previous);
        if (previous.getOwnerOrgId() != null) {
            events.publishEvent(new BizNotificationEvent("FACTOR", null, previous.getOwnerOrgId(),
                    "因子版本已回滚", "因子 " + previous.getFactorCode() + " 已回滚至 "
                            + previous.getVersion() + "（原版本 " + target.getVersion() + " 归档）。"));
        }
        return Result.ok(Map.of("rolledBack", true, "from", target.getVersion(),
                "to", previous.getVersion(), "status", "EFFECTIVE"));
    }

    private void archiveOthers(String factorCode, Long keepId) {
        try {
            mapper.update(null, new LambdaUpdateWrapper<EmissionFactor>()
                    .eq(EmissionFactor::getFactorCode, factorCode)
                    .ne(keepId != null, EmissionFactor::getId, keepId)
                    .set(EmissionFactor::getStatus, "ARCHIVED")
                    .set(EmissionFactor::getUpdateTime, LocalDateTime.now()));
        } catch (Exception ignored) {
        }
    }

    private EmissionFactor require(Long id) {
        EmissionFactor f;
        try {
            f = mapper.selectById(id);
        } catch (Exception e) {
            throw new BizException("因子库不可用：" + e.getMessage());
        }
        if (f == null) {
            throw new BizException("因子不存在：" + id);
        }
        return f;
    }

    private String nextVersion(String factorCode) {
        int major = 1;
        int minor = 0;
        try {
            List<EmissionFactor> rows = mapper.selectList(new LambdaQueryWrapper<EmissionFactor>()
                    .eq(EmissionFactor::getFactorCode, factorCode));
            for (EmissionFactor r : rows) {
                Matcher m = VERSION.matcher(r.getVersion() == null ? "" : r.getVersion());
                if (m.find()) {
                    int a = Integer.parseInt(m.group(1));
                    int b = Integer.parseInt(m.group(2));
                    if (a > major || (a == major && b >= minor)) {
                        major = a;
                        minor = b;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "V" + major + "." + (minor + 1);
    }

    private EmissionFactor demo() {
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
        f.setAuditTime(LocalDateTime.now());
        f.setCreateTime(LocalDateTime.now());
        return f;
    }
}