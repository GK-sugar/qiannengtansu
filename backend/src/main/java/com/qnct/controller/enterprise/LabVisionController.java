package com.qnct.controller.enterprise;

import com.qnct.common.Result;
import com.qnct.service.StorageGateway;
import com.qnct.service.VisionGateway;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 企业端 · 化验单识别（API-2 多模态 qwen3-vl-plus，无 Key 演示降级）
 */
@RestController
@RequestMapping("/api/v1/enterprise/lab-report")
public class LabVisionController {

    private final VisionGateway vision;
    private final StorageGateway storage;

    public LabVisionController(VisionGateway vision, StorageGateway storage) {
        this.vision = vision;
        this.storage = storage;
    }

    /** 化验单照片(Base64 dataUrl) → 结构化字段（演示模式返回内置样例） */
    @PostMapping("/recognize")
    public Result<Map<String, Object>> recognize(@RequestBody Map<String, String> body) {
        String image = body.getOrDefault("image", "");
        Map<String, Object> fields = vision.extractLabFields(image);
        if (image != null && !image.isBlank()) {
            fields.put("filePath", storage.saveBase64("lab", image));
        }
        return Result.ok(fields);
    }

    @PostMapping("/confirm")
    public Result<Map<String, Object>> confirm(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of("confirmed", true, "labNo", "HN-2026-" + System.currentTimeMillis() % 10000,
                "toCalc", "字段已入化验单库，可在核算模块引用"));
    }
}