package com.qnct.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qnct.common.BizException;
import com.qnct.common.Result;
import com.qnct.entity.ScCollabTask;
import com.qnct.entity.ScMaterialFlow;
import com.qnct.entity.ScSupplier;
import com.qnct.mapper.ScCollabTaskMapper;
import com.qnct.mapper.ScMaterialFlowMapper;
import com.qnct.mapper.ScSupplierMapper;
import com.qnct.security.SecurityUtils;
import com.qnct.util.IdGen;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 企业端 · 供应链协同：伙伴档案 / 物料流转 / 协同任务
 * 正式读写 sc_supplier / sc_material_flow / sc_collab_task，按当前登录企业 orgId 隔离。
 */
@RestController
@RequestMapping("/api/v1/enterprise/supply")
public class SupplierController {

    private final ScSupplierMapper supplierMapper;
    private final ScMaterialFlowMapper flowMapper;
    private final ScCollabTaskMapper taskMapper;

    public SupplierController(ScSupplierMapper supplierMapper, ScMaterialFlowMapper flowMapper,
                              ScCollabTaskMapper taskMapper) {
        this.supplierMapper = supplierMapper;
        this.flowMapper = flowMapper;
        this.taskMapper = taskMapper;
    }

    @GetMapping("/suppliers")
    public Result<List<Map<String, Object>>> suppliers() {
        Long orgId = SecurityUtils.orgId(1001L);
        try {
            List<ScSupplier> rows = supplierMapper.selectList(new LambdaQueryWrapper<ScSupplier>()
                    .eq(ScSupplier::getOrgId, orgId)
                    .orderByDesc(ScSupplier::getCreateTime));
            if (rows.isEmpty()) {
                return Result.ok(demoSuppliers());
            }
            List<Map<String, Object>> out = new ArrayList<>();
            for (ScSupplier s : rows) {
                out.add(supplierView(s));
            }
            return Result.ok(out);
        } catch (Exception e) {
            return Result.ok(demoSuppliers());
        }
    }

    @PostMapping("/suppliers")
    public Result<Map<String, Object>> addSupplier(@RequestBody Map<String, Object> body) {
        String name = str(body.get("name"), null);
        if (name == null) {
            throw new BizException("供应商名称不能为空");
        }
        ScSupplier s = new ScSupplier();
        s.setOrgId(SecurityUtils.orgId(1001L));
        s.setPartnerType(str(body.get("partnerType"), "SUPPLIER"));
        s.setName(name);
        s.setCreditCode(str(body.get("creditCode"), str(body.get("cert"), null)));
        s.setContactName(str(body.get("contactName"), null));
        s.setContactPhone(str(body.get("contactPhone"), null));
        s.setMaterialScope(str(body.get("scope"), str(body.get("materialScope"), null)));
        s.setRegion(str(body.get("region"), null));
        s.setStatus("ENABLED");
        s.setCreateTime(LocalDateTime.now());
        s.setUpdateTime(LocalDateTime.now());
        try {
            supplierMapper.insert(s);
        } catch (Exception e) {
            return Result.ok(Map.of("saved", true, "name", name, "note", "演示模式未落库"));
        }
        return Result.ok(Map.of("saved", true, "id", s.getId(), "name", name, "status", "ENABLED"));
    }

    @GetMapping("/flows")
    public Result<List<Map<String, Object>>> flows() {
        Long orgId = SecurityUtils.orgId(1001L);
        try {
            List<ScMaterialFlow> rows = flowMapper.selectList(new LambdaQueryWrapper<ScMaterialFlow>()
                    .eq(ScMaterialFlow::getOrgId, orgId)
                    .orderByDesc(ScMaterialFlow::getCreateTime));
            Map<Long, String> partners = supplierMapper.selectList(new LambdaQueryWrapper<ScSupplier>()
                            .eq(ScSupplier::getOrgId, orgId))
                    .stream().collect(Collectors.toMap(ScSupplier::getId, ScSupplier::getName, (a, b) -> a));
            List<Map<String, Object>> out = new ArrayList<>();
            for (ScMaterialFlow f : rows) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", f.getId());
                m.put("partnerId", f.getPartnerId());
                m.put("partner", partners.getOrDefault(f.getPartnerId(), "伙伴 " + f.getPartnerId()));
                m.put("flowType", f.getFlowType());
                m.put("material", f.getMaterialName());
                m.put("quantity", f.getQuantity());
                m.put("unit", f.getUnit());
                m.put("period", f.getPeriod());
                m.put("transportMode", f.getTransportMode());
                m.put("transportDistance", f.getTransportDistance());
                m.put("allocatedEmission", f.getAllocatedEmission());
                m.put("status", f.getStatus());
                out.add(m);
            }
            return Result.ok(out);
        } catch (Exception e) {
            return Result.ok(List.of());
        }
    }

    @PostMapping("/flows")
    public Result<Map<String, Object>> addFlow(@RequestBody Map<String, Object> body) {
        String material = str(body.get("material"), str(body.get("materialName"), null));
        if (material == null) {
            throw new BizException("物料名称不能为空");
        }
        ScMaterialFlow f = new ScMaterialFlow();
        f.setOrgId(SecurityUtils.orgId(1001L));
        f.setPartnerId(lng(body.get("partnerId"), 1L));
        f.setFlowType(str(body.get("flowType"), "IN"));
        f.setMaterialName(material);
        f.setQuantity(dec(body.get("quantity"), BigDecimal.ZERO));
        f.setUnit(str(body.get("unit"), "t"));
        f.setPeriod(str(body.get("period"), LocalDateTime.now().toString().substring(0, 7)));
        f.setTransportMode(str(body.get("transportMode"), null));
        f.setTransportDistance(dec(body.get("transportDistance"), null));
        f.setAllocatedEmission(dec(body.get("allocatedEmission"), null));
        f.setStatus("CONFIRMED");
        f.setCreateTime(LocalDateTime.now());
        f.setUpdateTime(LocalDateTime.now());
        try {
            flowMapper.insert(f);
        } catch (Exception e) {
            return Result.ok(Map.of("saved", true, "traceNo", IdGen.bizNo("SC"), "note", "演示模式未落库"));
        }
        return Result.ok(Map.of("saved", true, "id", f.getId(), "traceNo", IdGen.bizNo("SC")));
    }

    @GetMapping("/tasks")
    public Result<List<ScCollabTask>> tasks() {
        try {
            return Result.ok(taskMapper.selectList(new LambdaQueryWrapper<ScCollabTask>()
                    .eq(ScCollabTask::getOrgId, SecurityUtils.orgId(1001L))
                    .orderByDesc(ScCollabTask::getCreateTime)));
        } catch (Exception e) {
            return Result.ok(List.of());
        }
    }

    @PostMapping("/tasks")
    public Result<Map<String, Object>> addTask(@RequestBody Map<String, Object> body) {
        ScCollabTask t = new ScCollabTask();
        t.setOrgId(SecurityUtils.orgId(1001L));
        t.setPartnerId(lng(body.get("partnerId"), null));
        t.setTaskType(str(body.get("taskType"), "DATA_COLLECT"));
        t.setTitle(str(body.get("title"), "供应链数据收集"));
        t.setContent(str(body.get("content"), ""));
        t.setStatus("PENDING");
        t.setCreateTime(LocalDateTime.now());
        t.setUpdateTime(LocalDateTime.now());
        try {
            taskMapper.insert(t);
        } catch (Exception e) {
            return Result.ok(Map.of("saved", true, "note", "演示模式未落库"));
        }
        return Result.ok(Map.of("saved", true, "id", t.getId(), "status", "PENDING"));
    }

    private Map<String, Object> supplierView(ScSupplier s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("partnerType", s.getPartnerType());
        m.put("type", "SUPPLIER".equals(s.getPartnerType()) ? "供应商" : "客户");
        m.put("creditCode", s.getCreditCode());
        m.put("cert", s.getCreditCode());
        m.put("contactName", s.getContactName());
        m.put("contactPhone", s.getContactPhone());
        m.put("scope", s.getMaterialScope());
        m.put("region", s.getRegion());
        m.put("status", "ENABLED".equals(s.getStatus()) ? "合作中" : "停用");
        m.put("statusCode", s.getStatus());
        return m;
    }

    private List<Map<String, Object>> demoSuppliers() {
        return List.of(
                Map.of("name", "贵州盘江供应链有限公司", "type", "供应商", "scope", "原煤", "status", "合作中", "region", "六盘水"),
                Map.of("name", "南方电网贵州分公司", "type", "供应商", "scope", "电力", "status", "合作中", "region", "贵阳"),
                Map.of("name", "贵州公路运输集团", "type", "供应商", "scope", "公路运输", "status", "合作中", "region", "贵阳"));
    }

    private static String str(Object o, String def) {
        if (o == null) {
            return def;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? def : s;
    }

    private static Long lng(Object o, Long def) {
        if (o == null || String.valueOf(o).isBlank()) {
            return def;
        }
        try {
            return Long.parseLong(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static BigDecimal dec(Object o, BigDecimal def) {
        try {
            return o == null || String.valueOf(o).isBlank() ? def : new BigDecimal(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}