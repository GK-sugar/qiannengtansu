package com.qnct.service;

import java.util.List;
import java.util.Map;

/**
 * 大模型网关（API-1：AI 碳管家 / 初审理由 / 复核润色 / 整改计划生成）
 * OpenAI 兼容协议，默认阿里云百炼 qwen-plus。
 * 【此处需填入API Key】申请：https://bailian.console.aliyun.com/ → API-KEY → 回填 DASHSCOPE_API_KEY
 * 无 Key 自动降级：内置碳知识话术库 + 规则模板，保证全站可演示。
 */
public interface LlmGateway {

    /** 问答：优先走模型；无 Key 走知识库降级 */
    String chat(String question, List<Map<String, String>> history, String context);

    /** 整改计划生成：返回措施清单 + 预算行（无 Key 走模板引擎） */
    PlanReply rectifyPlan(String issueNo, String topic, String risk, String source);

    /** 是否配置了真实 Key（前端可据实验结果提示「真实模型」徽标） */
    boolean live();

    record PlanItem(String measure, String dept, String deadline, String pri) {}

    record BudgetRow(String cat, String basis, double qty, String unit, double price, double amount) {}

    record PlanReply(List<PlanItem> plan, List<BudgetRow> budget) {}
}