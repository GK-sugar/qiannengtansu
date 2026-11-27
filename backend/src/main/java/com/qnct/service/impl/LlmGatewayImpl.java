package com.qnct.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qnct.mapper.SysApiInvokeLogMapper;
import com.qnct.service.LlmGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * LLM 网关实现：
 *  key 为空 → 知识库降级（离线话术 + 模板引擎）；key 存在 → 阿里云百炼 OpenAI 兼容协议调用。
 */
@Slf4j
@Service
public class LlmGatewayImpl implements LlmGateway {

    @Value("${app.llm.api-key:}")
    private String apiKey;

    @Value("${app.llm.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${app.llm.model:qwen-plus}")
    private String model;

    private final ObjectMapper objectMapper;
    private final SysApiInvokeLogMapper invokeLogMapper;
    private final AtomicInteger fallbackCounter = new AtomicInteger();

    public LlmGatewayImpl(ObjectMapper objectMapper, SysApiInvokeLogMapper invokeLogMapper) {
        this.objectMapper = objectMapper;
        this.invokeLogMapper = invokeLogMapper;
    }

    @Override
    public boolean live() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String chat(String question, List<Map<String, String>> history, String context) {
        if (!live()) {
            return fallbackAnswer(question, context);
        }
        try {
            List<Map<String, String>> messages = new ArrayList<>(history == null ? List.of() : history);
            Map<String, String> sys = new LinkedHashMap<>();
            sys.put("role", "system");
            sys.put("content", "你是「黔能碳溯」平台的 AI 碳管家，面向贵州重点产业提供碳核算/政策/合规咨询。"
                    + "回答必须优先引用下方企业上下文中的真实数据（排放量、活动数据、因子值、预警），"
                    + "结合问题给出有针对性的分析与建议，不要泛泛而谈。"
                    + (context == null || context.isBlank() ? "" : "\n企业上下文：\n" + context));
            messages.add(0, sys);
            Map<String, String> q = new LinkedHashMap<>();
            q.put("role", "user");
            q.put("content", question);
            messages.add(q);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("temperature", 0.6);

            JsonNode resp = RestClient.create(baseUrl)
                    .post().uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve().body(JsonNode.class);

            String answer = resp == null ? fallbackAnswer(question, context)
                    : resp.path("choices").path(0).path("message").path("content").asText(fallbackAnswer(question, context));
            logInvoke(question, true, usageOf(resp));
            return answer;
        } catch (Exception e) {
            log.warn("LLM 调用失败，降级知识库：{}", e.getMessage());
            logInvoke(question, false, 0);
            return fallbackAnswer(question, context);
        }
    }

    private int usageOf(JsonNode resp) {
        return resp == null ? 0 : resp.path("usage").path("total_tokens").asInt(0);
    }

    private void logInvoke(String question, boolean ok, int tokens) {
        try {
            var row = new com.qnct.entity.SysApiInvokeLog();
            row.setApiType("LLM");
            row.setBizType("AI_CHAT");
            row.setRequestSummary(question.length() > 120 ? question.substring(0, 120) : question);
            row.setSuccess(ok ? 1 : 0);
            row.setCostTokens(tokens);
            row.setCreateTime(LocalDateTime.now());
            invokeLogMapper.insert(row);
        } catch (Exception ignored) {
        }
    }

    /** ---------- 离线知识库（无 Key 时基于知识库 + 数据库实况回答） ---------- */
    private String fallbackAnswer(String question, String context) {
        String q = question == null ? "" : question;
        String data = (context == null || context.isBlank()) ? ""
                : "\n\n【企业数据实况（来自数据库）】\n" + context;
        if (contains(q, "F1", "范围一", "化石燃料", "原煤", "柴油")) {
            return "F1 范围一直接排放：E = AD × NCV × EF × OF。AD 为燃料消耗量，NCV 为低位发热量(GJ/t)，"
                    + "EF 为热值排放因子(tCO2e/GJ)，OF 为氧化率(默认 1)。贵州煤矿以原煤、柴油为主源，"
                    + "建议优先采用化验单实测低位发热量替代缺省值，可显著降低核算不确定度。" + data;
        }
        if (contains(q, "F2", "范围二", "购电", "电力")) {
            return "F2 范围二间接排放：E = AD × EF_电网，AD 为外购电量(万kWh)，EF 为省级电网排放因子"
                    + "(当前版本 0.5703 tCO2e/MWh)。降低范围二排放的可行路径：绿电交易/分布式光伏自发自用、"
                    + "错峰用电与高效电机改造。请对照下方企业用电数据测算减排空间。" + data;
        }
        if (contains(q, "F3", "范围三", "运输", "物流")) {
            return "F3 范围三运输排放：E = Σ(货重 × 里程 × EF_运输)。公路重卡约 0.078 kgCO2e/t·km，"
                    + "铁路约 0.036、水运约 0.012 kgCO2e/t·km。贵州企业可借助乌江航道与铁路专用线，"
                    + "将大宗物料公路运输切换为铁水联运，单位运输排放可下降 50% 以上。" + data;
        }
        if (contains(q, "F6", "预警", "波动", "上升", "环比", "异常")) {
            return "F6 波动预警：ρ = |本期 − 基期| / 基期，默认阈值 ±20%，超出即列入异常清单并自动生成整改事项。"
                    + "归因时建议按「活动数据变化 × 因子变化」拆解：先核对消耗量环比，再核对因子版本是否切换。"
                    + "请对照下方各期排放数据定位波动来源。" + data;
        }
        if (contains(q, "F10", "哈希", "存证", "篡改")) {
            return "F10 哈希链存证：h(i) = SHA-256(salt ‖ h(i−1) ‖ SHA-256(data_i ‖ salt))。任一数据被篡改，"
                    + "本块及后续所有块指纹校验必失败，公开验真页可扫码复算。" + data;
        }
        if (contains(q, "CBAM", "欧盟", "出海", "碳边境")) {
            return "CBAM（欧盟碳边境调节机制）要求填报：吨产品直接排放、间接排放、碳价与免费配额。"
                    + "贵州钢铁、铝、氢等行业属过渡期范围，出海数据包需按 CBAM 模板拼装 Excel，"
                    + "且排放因子口径须与欧盟认可方法一致（建议保留化验单与因子来源证据链）。" + data;
        }
        if (contains(q, "CCER", "自愿减排", "碳资产", "金融", "信用", "授信")) {
            return "CCER（国家核证自愿减排量）是企业在强制履约之外进行碳资产交易的工具。平台绿色金融模块"
                    + "可基于历史核算与履约记录计算信用分和预授信额度(F7/F8)：连续按期报送、无高危预警、"
                    + "数据存证完整的企业信用分更高。" + data;
        }
        if (contains(q, "化验", "发热量", "检测")) {
            return "化验单低位发热量(Qnet,ar)直接决定范围一核算精度：E = 消耗量 × Qnet,ar × 单位热值含碳量 × 氧化率。"
                    + "采用实测值替代缺省值后，原煤核算结果通常可修正 3%~8%。平台支持化验单拍照识别自动录入，"
                    + "识别结果经双人复核后进入核算引擎。" + data;
        }
        return "已基于企业近期活动数据与省级因子库为您分析。您可以进一步问我：碳核算公式(F1~F5)、"
                + "排放波动归因(F6)、哈希链存证(F10)、CBAM 合规、CCER 与绿色金融，"
                + "或针对下方某一期数据让我给出具体减排建议。" + data;
    }

    private boolean contains(String src, String... keys) {
        for (String k : keys) {
            if (src.contains(k)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public PlanReply rectifyPlan(String issueNo, String topic, String risk, String source) {
        if (live()) {
            return rectifyByModel(issueNo, topic, risk, source);
        }
        return rectifyByTemplate(issueNo, topic, risk, source);
    }

    /** ---------- 整改计划模板引擎（与前端降级逻辑同构，便于无 Key 演示） ---------- */
    private PlanReply rectifyByTemplate(String issueNo, String topic, String risk, String source) {
        List<PlanItem> plan = new ArrayList<>();
        List<BudgetRow> budget = new ArrayList<>();
        String t = topic == null ? "" : topic;
        boolean energy = contains(t, "用电", "电力", "能耗");
        boolean ledger = contains(t, "台账", "磅单", "证据", "缺失");
        boolean factor = contains(t, "因子", "口径", "CBAM");
        boolean lab = contains(t, "化验", "发热量", "检测");
        boolean riskHigh = "高".equals(risk);

        if (ledger) {
            plan.add(new PlanItem("补齐能源台账及磅单佐证并归档扫描件", "能源管理部", "7 天", "高"));
            plan.add(new PlanItem("建立月度数据三级复核签字制度", "财务部/能源部", "15 天", "中"));
            budget.add(new BudgetRow("档案整理与扫描服务", "外包 2 人天", 2, "人天", 800, 1600));
            budget.add(new BudgetRow("台账管理系统补丁", "软件开发", 1, "项", 5000, 5000));
        }
        if (energy) {
            plan.add(new PlanItem("加载智能电表分项计量并月度比对", "设备部", "20 天", riskHigh ? "高" : "中"));
            plan.add(new PlanItem("制定错峰用电与节能改造方案", "生产调度中心", "30 天", "中"));
            budget.add(new BudgetRow("智能电表分项计量改造", riskHigh ? "12 个点位" : "6 个点位", riskHigh ? 12 : 6, "点位", 2600, riskHigh ? 31200 : 15600));
            budget.add(new BudgetRow("能效诊断服务", "第三方机构", 1, "项", 18000, 18000));
        }
        if (factor) {
            plan.add(new PlanItem("按 CBAM 最新口径重选运输排放因子并留证据链", "供应链管理部", "10 天", "高"));
            plan.add(new PlanItem("邀请核查机构做口径预审", "合规部", "20 天", "中"));
            budget.add(new BudgetRow("因子库比对与口径修正咨询", "咨询", 3, "人天", 3000, 9000));
        }
        if (lab) {
            plan.add(new PlanItem("补送 2 批次煤样至第三方实验室复核", "质检部", "5 天", "高"));
            plan.add(new PlanItem("化验单识别录入流程增加人工双人复核", "数字化部", "15 天", "中"));
            budget.add(new BudgetRow("煤质检测费", "2 批次 × 3 参数", 2, "批次", 1500, 3000));
        }
        if (plan.isEmpty()) {
            plan.add(new PlanItem("核对整改事项根因并制定专项整改方案", "能源管理部", "14 天", riskHigh ? "高" : "中"));
            plan.add(new PlanItem("整改完成后提交验证材料申请闭环", "数据管理员", "21 天", "中"));
            budget.add(new BudgetRow("专项整改施工", "据评估", 1, "项", 20000, 20000));
        }
        // 预算取整 + 附带 10% 预备费（由前端/控制器汇总展示）
        return new PlanReply(plan, budget);
    }

    private PlanReply rectifyByModel(String issueNo, String topic, String risk, String source) {
        try {
            String prompt = "你是工厂碳管理整改专家。针对整改事项[" + issueNo + "]：“" + topic
                    + "”（风险" + risk + "，来源：" + source + "），输出 JSON："
                    + "{\"plan\":[{\"measure\":\"措施\",\"dept\":\"责任部门\",\"deadline\":\"天数\",\"pri\":\"高/中\"}],"
                    + "\"budget\":[{\"cat\":\"科目\",\"basis\":\"依据\",\"qty\":数量,\"unit\":\"单位\",\"price\":单价}]}，"
                    + "只输出 JSON，不要解释。";
            List<Map<String, String>> msgs = List.of(Map.of("role", "user", "content", prompt));
            String answer = chat(prompt, null, null);
            JsonNode root = objectMapper.readTree(extractJson(answer));
            List<PlanItem> plan = new ArrayList<>();
            for (JsonNode n : root.path("plan")) {
                plan.add(new PlanItem(n.path("measure").asText(), n.path("dept").asText(),
                        n.path("deadline").asText(), n.path("pri").asText()));
            }
            List<BudgetRow> budget = new ArrayList<>();
            for (JsonNode n : root.path("budget")) {
                double qty = n.path("qty").asDouble();
                double price = n.path("price").asDouble();
                budget.add(new BudgetRow(n.path("cat").asText(), n.path("basis").asText(),
                        qty, n.path("unit").asText(), price, qty * price));
            }
            if (plan.isEmpty()) {
                return rectifyByTemplate(issueNo, topic, risk, source);
            }
            return new PlanReply(plan, budget);
        } catch (Exception e) {
            return rectifyByTemplate(issueNo, topic, risk, source);
        }
    }

    private String extractJson(String raw) {
        int a = raw.indexOf('{');
        int b = raw.lastIndexOf('}');
        return (a >= 0 && b > a) ? raw.substring(a, b + 1) : "{}";
    }
}