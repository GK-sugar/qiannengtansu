package com.qnct.controller.enterprise;

import com.qnct.common.Result;
import com.qnct.dto.RectifyGenerateRequest;
import com.qnct.entity.EntRectifyIssue;
import com.qnct.event.BizNotificationEvent;
import com.qnct.mapper.EntRectifyIssueMapper;
import com.qnct.security.SecurityUtils;
import com.qnct.service.HashChainService;
import com.qnct.service.LlmGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业端 · 整改计划生成与预算预估（最新模块）：
 *   GET  /issues          整改事项清单（初审退回/波动预警/核查退回）
 *   POST /generate        AI 生成措施清单 + 预算行（LLM 优先，无 Key 走模板）
 *   POST /budget/estimate 预算预估：各分项小计 + 10% 预备费
 *   POST /submit          提交审批并上链存证（F10）
 */
@RestController
@RequestMapping("/api/v1/enterprise/rectify")
public class RectifyController {

    private final LlmGateway llm;
    private final HashChainService hashChain;
    private final EntRectifyIssueMapper issueMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher events;

    public RectifyController(LlmGateway llm, HashChainService hashChain,
                             EntRectifyIssueMapper issueMapper, ObjectMapper objectMapper,
                             ApplicationEventPublisher events) {
        this.llm = llm;
        this.hashChain = hashChain;
        this.issueMapper = issueMapper;
        this.objectMapper = objectMapper;
        this.events = events;
    }

    @GetMapping("/issues")
    public Result<List<Map<String, Object>>> issues() {
        try {
            var rows = issueMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EntRectifyIssue>()
                        .eq(EntRectifyIssue::getOrgId, SecurityUtils.orgId(1001L))
                        .orderByDesc(EntRectifyIssue::getCreateTime));
            if (rows.isEmpty()) {
                return Result.ok(demoIssues());
            }
            return Result.ok(rows.stream().map(r -> Map.<String, Object>of(
                    "issueNo", r.getIssueNo(), "source", r.getSourceType(), "topic", r.getTopic(),
                    "risk", r.getRiskLevel(), "status", r.getStatus())).toList());
        } catch (Exception e) {
            return Result.ok(demoIssues());
        }
    }

    private List<Map<String, Object>> demoIssues() {
        return List.of(
                Map.of("issueNo", "ZG-2026-041", "source", "初审退回", "topic", "上月原煤消耗台账缺磅单佐证，数据完整性不足", "risk", "高", "status", "待整改"),
                Map.of("issueNo", "ZG-2026-039", "source", "波动预警", "topic", "范围二购电排放环比 +27.4%，超出 ±20% 阈值", "risk", "高", "status", "待整改"),
                Map.of("issueNo", "ZG-2026-035", "source", "核查退回", "topic", "运输因子选用与 CBAM 口径不一致，需补充里程证据链", "risk", "中", "status", "整改中"),
                Map.of("issueNo", "ZG-2026-030", "source", "波动预警", "topic", "化验单发热量字段缺失 2 批次，影响 F2 核算参数", "risk", "中", "status", "已闭环"));
    }

    /** AI 生成：LLM（【此处需填入API Key】）→ 降级模板引擎 */
    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@Valid @RequestBody RectifyGenerateRequest req) {
        LlmGateway.PlanReply reply = llm.rectifyPlan(req.issueNo(), req.topic(), req.risk(), req.source());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("issueNo", req.issueNo());
        out.put("plan", reply.plan());
        out.put("budget", reply.budget());
        out.put("live", llm.live());
        return Result.ok(out);
    }

    /** 预算预估：小计 + 10% 预备费（四舍五入到元） */
    @PostMapping("/budget/estimate")
    public Result<Map<String, Object>> estimate(@RequestBody Map<String, Object> body) {
        try {
            List<LlmGateway.BudgetRow> rows = objectMapper.convertValue(body.get("rows"),
                    new TypeReference<List<LlmGateway.BudgetRow>>() {});
            BigDecimal subtotal = BigDecimal.ZERO;
            List<Map<String, Object>> detail = new ArrayList<>();
            for (LlmGateway.BudgetRow r : rows) {
                BigDecimal amount = BigDecimal.valueOf(r.qty()).multiply(BigDecimal.valueOf(r.price()))
                        .setScale(2, RoundingMode.HALF_UP);
                subtotal = subtotal.add(amount);
                detail.add(Map.of("cat", r.cat(), "qty", r.qty(), "unit", r.unit(), "price", r.price(), "amount", amount));
            }
            BigDecimal reserve = subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("subtotal", subtotal);
            out.put("reserve", reserve);
            out.put("total", subtotal.add(reserve));
            out.put("detail", detail);
            out.put("rule", "各分项小计 + 10% 预备费（不可预见支出）");
            return Result.ok(out);
        } catch (Exception e) {
            return Result.fail(400, "预算行解析失败：" + e.getMessage());
        }
    }

    /** 提交整改计划并上链存证 */
    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestBody Map<String, Object> body) {
        String issueNo = String.valueOf(body.getOrDefault("issueNo", "ZG-2026-041"));
        Long orgId = SecurityUtils.orgId(1001L);
        Long operatorId = SecurityUtils.userId(4L);
        Long issueId = null;
        try {
            String planJson = objectMapper.writeValueAsString(body.get("plan"));
            String budgetJson = objectMapper.writeValueAsString(body.get("budget"));
            EntRectifyIssue issue = new EntRectifyIssue();
            issue.setIssueNo(issueNo);
            issue.setOrgId(orgId);
            issue.setSourceType(String.valueOf(body.getOrDefault("source", "波动预警")));
            issue.setTopic(String.valueOf(body.getOrDefault("topic", "")));
            issue.setRiskLevel(String.valueOf(body.getOrDefault("risk", "中")));
            issue.setStatus("整改中");
            issue.setPlanJson(planJson);
            issue.setBudgetJson(budgetJson);
            issue.setCreateTime(LocalDateTime.now());
            issue.setUpdateTime(LocalDateTime.now());
            issueMapper.insert(issue);
            issueId = issue.getId();
        } catch (Exception e) {
            // 落库失败仅演示
        }
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("issueNo", issueNo);
        snap.put("orgId", orgId);
        snap.put("plan", body.get("plan"));
        snap.put("budget", body.get("budget"));
        snap.put("status", "整改中");
        var block = hashChain.append("RECTIFY", issueId == null ? 202611001L : issueId, issueNo, snap, operatorId);
        events.publishEvent(new BizNotificationEvent("RECTIFY", null, 1L,
                "企业提交整改计划", "企业 " + orgId + " 提交了整改事项 " + issueNo
                        + " 的措施与预算，已上链存证（区块 #" + block.getBlockSeq() + "），待审核。"));
        return Result.ok(Map.of("submitted", true, "blockSeq", block.getBlockSeq(), "hash", block.getDataHash()));
    }
}