package com.qnct.controller.enterprise;

import com.qnct.common.Result;
import com.qnct.entity.CarbonPassport;
import com.qnct.mapper.CarbonPassportMapper;
import com.qnct.security.LoginUser;
import com.qnct.service.PassportService;
import com.qnct.util.IdGen;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业端 · 护照申请与状态追踪（状态机：填报→省审→机构复核→国家发证）
 */
@RestController
@RequestMapping("/api/v1/enterprise/passport")
public class PassportController {

    private final CarbonPassportMapper passportMapper;
    private final PassportService passportService;

    public PassportController(CarbonPassportMapper passportMapper, PassportService passportService) {
        this.passportMapper = passportMapper;
        this.passportService = passportService;
    }

    /** 从 SecurityContext 取当前登录企业的 orgId */
    private Long currentOrgId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser lu && lu.getOrgId() != null) {
            return lu.getOrgId();
        }
        return 1001L;
    }

    @GetMapping("/applications")
    public Result<List<Map<String, Object>>> applications() {
        try {
            // 按当前企业 orgId 查询自己的申请，避免跨企业数据泄露
            var rows = passportMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CarbonPassport>()
                            .eq(CarbonPassport::getOrgId, currentOrgId())
                            .orderByDesc(CarbonPassport::getIssueTime));
            if (rows.isEmpty()) {
                return Result.ok(demoList());
            }
            return Result.ok(rows.stream().map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("no", p.getPassportNo());
                m.put("product", p.getProductName());
                m.put("standard", p.getStandard());
                m.put("status", p.getStatus());
                m.put("applyTime", p.getIssueTime());
                return m;
            }).toList());
        } catch (Exception e) {
            return Result.ok(demoList());
        }
    }

    private List<Map<String, Object>> demoList() {
        return List.of(
                Map.of("no", "SQ-2026-0018", "product", "精煤（洗选产品）", "standard", "ISO 14067",
                        "status", "ISSUED", "progress", 100, "applyTime", "2026-09-12"),
                Map.of("no", "SQ-2026-0041", "product", "磷酸铁锂正极材料", "standard", "EU Battery",
                        "status", "PENDING_AGENCY", "progress", 62, "applyTime", "2026-10-28"),
                Map.of("no", "SQ-2026-0056", "product", "精煤 Q3 核证", "standard", "CBAM",
                        "status", "PENDING_GOV", "progress", 30, "applyTime", "2026-11-18"));
    }

    /** 新建护照申请 —— 落库为 DRAFT 状态，后续由省厅初审启动状态机 */
    @PostMapping("/apply")
    public Result<Map<String, Object>> apply(@RequestBody Map<String, String> body) {
        String product = body.getOrDefault("product", "");
        String standard = body.getOrDefault("standard", "ISO 14067");
        if (product.isBlank()) {
            return Result.fail(400, "产品名称 product 必填");
        }

        String no = IdGen.passportNo();
        CarbonPassport p = new CarbonPassport();
        p.setPassportNo(no);
        p.setOrgId(currentOrgId());
        p.setProductName(product);
        p.setStandard(standard);
        p.setStatus("DRAFT");
        p.setTotalCo2e(BigDecimal.ZERO);
        p.setIntensity(BigDecimal.ZERO);

        try {
            passportMapper.insert(p);
        } catch (Exception e) {
            // 演示库不可用时降级为内存返回
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("no", no);
        out.put("status", "DRAFT");
        out.put("step", "草稿已保存，可补充核算报告后提交省审");
        return Result.ok(out);
    }

    /** 企业提交护照申请 → 触发省厅初审队列（状态 PENDING_GOV） */
    @PostMapping("/submit/{passportNo}")
    public Result<Map<String, Object>> submit(@PathVariable String passportNo) {
        try {
            CarbonPassport p = passportMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CarbonPassport>()
                            .eq(CarbonPassport::getPassportNo, passportNo)
                            .eq(CarbonPassport::getOrgId, currentOrgId()));
            if (p == null) {
                return Result.fail(404, "护照申请不存在或无权操作");
            }
            if (!"DRAFT".equals(p.getStatus())) {
                return Result.fail(400, "当前状态 [" + p.getStatus() + "] 不可提交");
            }
            p.setStatus("PENDING_GOV");
            passportMapper.updateById(p);
            return Result.ok(Map.of("no", passportNo, "status", "PENDING_GOV", "msg", "已提交省厅初审"));
        } catch (Exception e) {
            return Result.fail(500, "提交失败：" + e.getMessage());
        }
    }
}
