package com.qnct.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 多模态视觉网关（API-2：化验单识别 qwen3-vl-plus）
 * 有 Key → 百炼 OpenAI 兼容协议携带图片真实读图；无 Key / 调用失败 → 内置样例字段（demo=true）。
 */
@Slf4j
@Service
public class VisionGateway {

    @Value("${app.llm.api-key:}")
    private String apiKey;

    @Value("${app.llm.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${app.llm.vl-model:qwen3-vl-plus}")
    private String vlModel;

    private final ObjectMapper objectMapper;

    public VisionGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 化验单字段：灰分/硫分/低位发热量等 */
    public Map<String, Object> extractLabFields(String base64Image) {
        if (apiKey == null || apiKey.isBlank() || base64Image == null || base64Image.isBlank()) {
            return demoFields();
        }
        try {
            String dataUrl = base64Image.startsWith("data:") ? base64Image
                    : "data:image/jpeg;base64," + base64Image;
            String prompt = "你是煤质化验单识别专家。请读取图片中的化验单/检测报告，提取以下字段并只输出 JSON，不要解释："
                    + "{\"coalName\":\"煤种\",\"ash\":灰分Aad百分数数字,\"sulfur\":全硫St,d百分数数字,"
                    + "\"ncv\":低位发热量Qnet,ar数字(单位GJ/t，若单上为MJ/kg数值相同，若为kcal/kg请除以239换算),"
                    + "\"moisture\":全水分Mt百分数数字,\"volatile\":挥发分Vdaf百分数数字,"
                    + "\"reportNo\":\"报告编号\",\"confidence\":0到1的识别置信度}。"
                    + "图中没有的字段填 null，数值保留两位小数。";
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", vlModel);
            body.put("messages", List.of(Map.of("role", "user", "content", List.of(
                    Map.of("type", "image_url", "image_url", Map.of("url", dataUrl)),
                    Map.of("type", "text", "text", prompt)))));
            body.put("temperature", 0.1);

            JsonNode resp = RestClient.create(baseUrl)
                    .post().uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve().body(JsonNode.class);
            String text = resp == null ? "" : resp.path("choices").path(0).path("message").path("content").asText("");
            JsonNode fields = objectMapper.readTree(extractJson(text));
            if (fields.isMissingNode() || fields.isEmpty()) {
                return demoFields();
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("demo", false);
            m.put("reportNo", fields.path("reportNo").asText(null));
            m.put("coalName", fields.path("coalName").asText(null));
            m.put("ash", num(fields.path("ash")));
            m.put("sulfur", num(fields.path("sulfur")));
            m.put("ncv", num(fields.path("ncv")));
            m.put("moisture", num(fields.path("moisture")));
            m.put("volatile", num(fields.path("volatile")));
            m.put("confidence", fields.path("confidence").asDouble(0.95));
            return m;
        } catch (Exception e) {
            log.warn("qwen3-vl-plus 读图失败，降级内置样例：{}", e.getMessage());
            return demoFields();
        }
    }

    private Object num(JsonNode n) {
        return n.isNumber() ? Math.round(n.asDouble() * 100) / 100.0 : null;
    }

    private String extractJson(String raw) {
        int a = raw.indexOf('{');
        int b = raw.lastIndexOf('}');
        return (a >= 0 && b > a) ? raw.substring(a, b + 1) : "{}";
    }

    private Map<String, Object> demoFields() {
        Map<String, Object> m = new LinkedHashMap<>();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        m.put("demo", true);
        m.put("reportNo", "HN-2026-" + String.format("%04d", r.nextInt(1, 9999)));
        m.put("coalName", r.nextBoolean() ? "焦煤" : "无烟煤");
        m.put("ash", round(r.nextDouble(8.5, 14.5)));
        m.put("sulfur", round(r.nextDouble(0.35, 1.60)));
        m.put("ncv", round(r.nextDouble(21.0, 28.2)));
        m.put("moisture", round(r.nextDouble(4.0, 9.0)));
        m.put("volatile", round(r.nextDouble(6.0, 14.0)));
        m.put("confidence", round(r.nextDouble(0.91, 0.99)));
        return m;
    }

    private double round(double v) {
        return Math.round(v * 10000) / 10000.0;
    }
}