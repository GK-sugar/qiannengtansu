package com.qnct.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qnct.dto.ChatRequest;
import com.qnct.entity.EmissionFactor;
import com.qnct.entity.EntActivityData;
import com.qnct.entity.GovAnomalyAlert;
import com.qnct.mapper.EmissionFactorMapper;
import com.qnct.mapper.EntActivityDataMapper;
import com.qnct.mapper.GovAnomalyAlertMapper;
import com.qnct.service.LlmGateway;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 企业端 · AI 碳管家（SSE 流式）
 * 回答前先从数据库组装企业实况上下文（活动数据排放、现行因子、待处理预警），
 * 有 Key 时交给 qwen-plus 结合数据推理；无 Key 时知识库降级同样携带数据实况。
 */
@RestController
@RequestMapping("/api/v1/enterprise/ai")
public class AiController {

    private final LlmGateway llm;
    private final EntActivityDataMapper activityMapper;
    private final EmissionFactorMapper factorMapper;
    private final GovAnomalyAlertMapper alertMapper;

    public AiController(LlmGateway llm, EntActivityDataMapper activityMapper,
                        EmissionFactorMapper factorMapper, GovAnomalyAlertMapper alertMapper) {
        this.llm = llm;
        this.activityMapper = activityMapper;
        this.factorMapper = factorMapper;
        this.alertMapper = alertMapper;
    }

    /** SSE：data: {delta:"..."}，结束 data: {done:true, live:boolean} */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody ChatRequest req) {
        SseEmitter emitter = new SseEmitter(120_000L);
        String context = mergeContext(req.context(), buildDbContext());
        String answer = llm.chat(req.message(), req.history() == null ? null
                        : req.history().stream().map(h -> Map.of("role", h.role(), "content", h.content())).toList(),
                context);
        // 拆句模拟流式输出（真实 Key 场景可改为边收边发）
        new Thread(() -> {
            try {
                int step = 24;
                for (int i = 0; i < answer.length(); i += step) {
                    String chunk = answer.substring(i, Math.min(answer.length(), i + step));
                    emitter.send(SseEmitter.event().name("message").data(Map.of("delta", chunk)));

                    Thread.sleep(40);
                }
                emitter.send(SseEmitter.event().name("message").data(Map.of("done", true, "live", llm.live())));
                emitter.complete();
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        }).start();
        return emitter;
    }

    /** 从数据库组装企业实况上下文；数据库不可用时返回空串，不影响问答主流程 */
    private String buildDbContext() {
        StringBuilder sb = new StringBuilder();
        try {
            List<EntActivityData> acts = activityMapper.selectList(
                    new QueryWrapper<EntActivityData>().orderByDesc("period").last("LIMIT 120"));
            if (!acts.isEmpty()) {
                // 各期 × 各范围排放合计（kg → t）
                Map<String, BigDecimal> sum = new TreeMap<>(Comparator.reverseOrder());
                for (EntActivityData a : acts) {
                    if (a.getPeriod() == null || a.getEmissionCo2e() == null) continue;
                    sum.merge(a.getPeriod() + " 范围" + a.getScope(), a.getEmissionCo2e(), BigDecimal::add);
                }
                sb.append("【数据库·近期排放(tCO2e)】");
                sum.forEach((k, v) -> sb.append(k).append('=')
                        .append(v.divide(BigDecimal.valueOf(1000), 1, RoundingMode.HALF_UP).toPlainString()).append('；'));
                // 最新一期主要活动数据明细
                String latest = acts.get(0).getPeriod();
                sb.append("【最新期 ").append(latest).append(" 活动数据】");
                acts.stream().filter(a -> latest.equals(a.getPeriod())).limit(8).forEach(a ->
                        sb.append(a.getActivityName()).append(' ').append(a.getQuantity()).append(a.getUnit())
                                .append("(排放").append(a.getEmissionCo2e().divide(BigDecimal.valueOf(1000), 1, RoundingMode.HALF_UP))
                                .append("t,范围").append(a.getScope()).append(')').append('；'));
            }
        } catch (Exception ignored) {
        }
        try {
            List<EmissionFactor> factors = factorMapper.selectList(
                    new QueryWrapper<EmissionFactor>().eq("status", "EFFECTIVE").last("LIMIT 8"));
            if (!factors.isEmpty()) {
                sb.append("【现行排放因子】");
                factors.forEach(f -> sb.append(f.getFactorName()).append('=').append(f.getFactorValue())
                        .append(f.getUnit()).append('；'));
            }
        } catch (Exception ignored) {
        }
        try {
            Long pending = alertMapper.selectCount(
                    new QueryWrapper<GovAnomalyAlert>().eq("status", "PENDING"));
            sb.append("【待处理预警】").append(pending == null ? 0 : pending).append(" 条");
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    private String mergeContext(String fromClient, String fromDb) {
        if (fromClient == null || fromClient.isBlank()) return fromDb;
        if (fromDb.isBlank()) return fromClient;
        return fromClient + "\n" + fromDb;
    }
}