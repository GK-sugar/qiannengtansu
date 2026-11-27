package com.qnct.controller.gov;

import com.qnct.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 省厅端 · 碳排放态势感知大屏（市州汇总 + 行业排行 + 滚动事件）
 */
@RestController
@RequestMapping("/api/v1/gov/screen")
public class ScreenController {

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        return Result.ok(Map.of(
                "enterprises", 2389,
                "covered", "92.7%",
                "totalEmission", 41268.5,
                "yearOnYear", -3.2,
                "pendingReviews", 186,
                "highAlerts", 14));
    }

    @GetMapping("/by-city")
    public Result<List<Map<String, Object>>> byCity() {
        return Result.ok(List.of(
                Map.of("name", "贵阳市", "value", 12680.2),
                Map.of("name", "六盘水市", "value", 10236.7),
                Map.of("name", "遵义市", "value", 6804.1),
                Map.of("name", "黔南州", "value", 4465.9),
                Map.of("name", "毕节市", "value", 3988.6),
                Map.of("name", "黔西南州", "value", 3092.9),
                Map.of("name", "安顺市", "value", 2541.3),
                Map.of("name", "黔东南州", "value", 1980.7),
                Map.of("name", "铜仁市", "value", 1478.1)));
    }

    @GetMapping("/ranking")
    public Result<List<Map<String, Object>>> ranking() {
        return Result.ok(List.of(
                Map.of("name", "煤炭开采和洗选业", "value", 18462.0, "ratio", 44.7),
                Map.of("name", "电力行业", "value", 11034.5, "ratio", 26.7),
                Map.of("name", "电解铝", "value", 6472.2, "ratio", 15.7),
                Map.of("name", "磷化工", "value", 3291.8, "ratio", 8.0),
                Map.of("name", "其他", "value", 2008.0, "ratio", 4.9)));
    }

    @GetMapping("/events")
    public Result<List<Map<String, Object>>> events() {
        return Result.ok(List.of(
                Map.of("time", "10:42", "text", "六盘水盘州矿区 3 家企业提交 10 月核算数据"),
                Map.of("time", "10:15", "text", "贵阳高新区新能源材料集群强度因子更新通过省级审核"),
                Map.of("time", "09:58", "text", "遵义铝业范围一排放环比 +18.6%，系统触发预警"),
                Map.of("time", "09:31", "text", "核查机构签发护照 QN-PP-2026-0041，链上存证 +1")));
    }
}