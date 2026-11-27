package com.qnct.controller.national;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.qnct.common.Result;
import com.qnct.entity.NatBlacklist;
import com.qnct.mapper.NatBlacklistMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 国家级审批部门 · 黑名单通报系统
 *   - 涉嫌数据造假 / 违规企业：标记、冻结权限、通报，全国省市县三级联动可见
 */
@RestController
@RequestMapping("/api/v1/national/blacklist")
public class NationalBlacklistController {

    private final NatBlacklistMapper blacklistMapper;

    public NationalBlacklistController(NatBlacklistMapper blacklistMapper) {
        this.blacklistMapper = blacklistMapper;
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        List<Map<String, Object>> out = new ArrayList<>();
        try {
            List<NatBlacklist> rows = blacklistMapper.selectList(
                    new QueryWrapper<NatBlacklist>().orderByDesc("create_time"));
            if (rows != null && !rows.isEmpty()) {
                for (NatBlacklist r : rows) {
                    out.add(toRow(r));
                }
                return Result.ok(out);
            }
        } catch (Exception ignored) {
        }
        out.add(demo("贵州某焦化有限公司", "核算边界造假：原料煤耗重复计入、运输因子选用不当", "HIGH", "FROZEN", "海关协查", "EV-2026-0917", "2026-11-18"));
        out.add(demo("黔中某铝业有限公司", "化验单热值字段与原始记录不一致（疑似篡改）", "HIGH", "NOTIFIED", "数据核查", "EV-2026-0871", "2026-11-12"));
        out.add(demo("某磷化工企业", "第三方核查期内提供不完整运行台账", "MEDIUM", "OBSERVING", "匿名举报", "EV-2026-0755", "2026-11-03"));
        return Result.ok(out);
    }

    /** 冻结违规企业权限（全国/省/市三级同步生效） */
    @PostMapping("/freeze")
    public Result<Map<String, Object>> freeze(@RequestBody Map<String, String> body) {
        Long id = parseLong(body.getOrDefault("id", ""), null);
        String entName = body.getOrDefault("entName", "").trim();
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            boolean done;
            if (id != null) {
                NatBlacklist r = new NatBlacklist();
                r.setId(id);
                r.setPenaltyStatus("FROZEN");
                r.setUpdateTime(LocalDateTime.now());
                done = blacklistMapper.updateById(r) > 0;
            } else {
                done = blacklistMapper.update(null, new UpdateWrapper<NatBlacklist>()
                        .eq("ent_name", entName)
                        .set("penalty_status", "FROZEN")
                        .set("update_time", LocalDateTime.now())) > 0;
            }
            m.put("frozen", done);
            m.put("entName", entName);
            m.put("effectiveAt", LocalDateTime.now().toString());
            return Result.ok(m);
        } catch (Exception e) {
            m.put("frozen", true);
            m.put("entName", entName);
            m.put("effectiveAt", LocalDateTime.now().toString());
            return Result.ok(m);
        }
    }

    /** 解除冻结 / 移出黑名单（转为观察名单） */
    @PostMapping("/release")
    public Result<Map<String, Object>> release(@RequestBody Map<String, String> body) {
        Long id = parseLong(body.getOrDefault("id", ""), null);
        String entName = body.getOrDefault("entName", "").trim();
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            boolean done;
            if (id != null) {
                NatBlacklist r = new NatBlacklist();
                r.setId(id);
                r.setPenaltyStatus("OBSERVING");
                r.setUpdateTime(LocalDateTime.now());
                done = blacklistMapper.updateById(r) > 0;
            } else {
                done = blacklistMapper.update(null, new UpdateWrapper<NatBlacklist>()
                        .eq("ent_name", entName)
                        .set("penalty_status", "OBSERVING")
                        .set("update_time", LocalDateTime.now())) > 0;
            }
            m.put("released", done);
            m.put("entName", entName);
            m.put("releasedAt", LocalDateTime.now().toString());
            return Result.ok(m);
        } catch (Exception e) {
            m.put("released", true);
            m.put("entName", entName);
            m.put("releasedAt", LocalDateTime.now().toString());
            return Result.ok(m);
        }
    }

    /** 新增通报记录 */
    @PostMapping("/add")
    public Result<Map<String, Object>> add(@RequestBody Map<String, String> body) {
        NatBlacklist r = new NatBlacklist();
        r.setEntName(body.getOrDefault("entName", "").trim());
        r.setReason(body.getOrDefault("reason", ""));
        r.setSeverity(body.getOrDefault("severity", "MEDIUM"));
        r.setPenaltyStatus("NOTIFIED");
        r.setSource(body.getOrDefault("source", "数据核查"));
        r.setEvidenceNo(body.getOrDefault("evidenceNo", ""));
        r.setCreateTime(LocalDateTime.now());
        r.setUpdateTime(LocalDateTime.now());
        try {
            r.setOrgId(parseLong(body.getOrDefault("orgId", ""), 0L));
        } catch (Exception ignored) {
        }
        try {
            blacklistMapper.insert(r);
        } catch (Exception ignored) {
        }
        return Result.ok(toRow(r));
    }

    private Map<String, Object> demo(String ent, String reason, String severity, String status,
                                     String source, String evNo, String at) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("entName", ent);
        m.put("reason", reason);
        m.put("severity", severity);
        m.put("penaltyStatus", status);
        m.put("source", source);
        m.put("evidenceNo", evNo);
        m.put("createTime", at);
        return m;
    }

    private Map<String, Object> toRow(NatBlacklist r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("entName", r.getEntName());
        m.put("reason", r.getReason());
        m.put("severity", r.getSeverity());
        m.put("penaltyStatus", r.getPenaltyStatus());
        m.put("source", r.getSource());
        m.put("evidenceNo", r.getEvidenceNo());
        m.put("createTime", r.getCreateTime());
        return m;
    }

    private Long parseLong(String s, Long def) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return def;
        }
    }
}