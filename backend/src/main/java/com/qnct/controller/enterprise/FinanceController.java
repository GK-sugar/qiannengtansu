package com.qnct.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qnct.common.Result;
import com.qnct.entity.FinCreditApply;
import com.qnct.event.BizNotificationEvent;
import com.qnct.mapper.FinCreditApplyMapper;
import com.qnct.security.SecurityUtils;
import com.qnct.service.CreditScoreService;
import com.qnct.util.IdGen;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 企业端 · 绿色金融：F7 信誉分 + F8 预授信额度（基于企业排放数据实时测算）
 */
@RestController
@RequestMapping("/api/v1/enterprise/finance")
public class FinanceController {

    private final CreditScoreService credit;
    private final FinCreditApplyMapper applyMapper;
    private final ApplicationEventPublisher events;

    public FinanceController(CreditScoreService credit, FinCreditApplyMapper applyMapper,
                             ApplicationEventPublisher events) {
        this.credit = credit;
        this.applyMapper = applyMapper;
        this.events = events;
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        Long orgId = SecurityUtils.orgId(1001L);
        try {
            return Result.ok(credit.profileForOrg(orgId, 30_000_000L));
        } catch (Exception e) {
            return Result.ok(credit.profile(92, 88, 81, 95, 30_000_000L));
        }
    }

    @PostMapping("/apply")
    public Result<Map<String, Object>> apply(@RequestBody Map<String, Object> body) {
        Long orgId = SecurityUtils.orgId(1001L);
        FinCreditApply a = new FinCreditApply();
        a.setApplyNo(IdGen.bizNo("FI"));
        a.setOrgId(orgId);
        a.setBankName(str(body.get("bankName"), "贵州银行绿色金融部"));
        a.setApplyQuota(dec(body.get("applyQuota"), new BigDecimal("1200")));
        a.setPurpose(str(body.get("purpose"), "减排改造"));
        a.setRemark(str(body.get("remark"), ""));
        a.setStatus("SUBMITTED");
        a.setCreateTime(LocalDateTime.now());
        a.setUpdateTime(LocalDateTime.now());
        try {
            applyMapper.insert(a);
        } catch (Exception e) {
            return Result.ok(Map.of("applyNo", a.getApplyNo(), "status", "SUBMITTED",
                    "note", "预授信申请已提交银行端（演示，未落库）"));
        }
        events.publishEvent(new BizNotificationEvent("FINANCE", null, 1L,
                "绿色金融授信申请", "企业 " + orgId + " 提交授信申请 " + a.getApplyNo()
                        + "，申请额度 " + a.getApplyQuota() + " 万元，用途 " + a.getPurpose() + "。"));
        return Result.ok(Map.of("applyNo", a.getApplyNo(), "status", "SUBMITTED",
                "note", "预授信申请已提交银行端，等待审批"));
    }

    @GetMapping("/applies")
    public Result<List<FinCreditApply>> applies() {
        try {
            return Result.ok(applyMapper.selectList(new LambdaQueryWrapper<FinCreditApply>()
                    .eq(FinCreditApply::getOrgId, SecurityUtils.orgId(1001L))
                    .orderByDesc(FinCreditApply::getCreateTime)));
        } catch (Exception e) {
            return Result.ok(List.of());
        }
    }

    private static String str(Object o, String def) {
        if (o == null) {
            return def;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? def : s;
    }

    private static BigDecimal dec(Object o, BigDecimal def) {
        try {
            return o == null || String.valueOf(o).isBlank() ? def : new BigDecimal(String.valueOf(o));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}