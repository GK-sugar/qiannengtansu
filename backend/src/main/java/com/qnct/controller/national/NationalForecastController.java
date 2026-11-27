package com.qnct.controller.national;

import com.qnct.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 国家级审批部门 · 宏观预测驾驶舱
 *   - 全国/省域历史碳排放与 2030 前预测曲线（内置线性外推 + 情景加权，无需外部 AI Key）
 *   - 行业结构、区域强度排行、达峰研判结论
 */
@RestController
@RequestMapping("/api/v1/national/forecast")
public class NationalForecastController {

    /** 全国宏观驾驶舱：历史 + 预测 + 行业结构 + 研判结论 */
    @GetMapping("/macro")
    public Result<Map<String, Object>> macro() {
        List<Double> history = List.of(9.86, 9.92, 10.21, 10.48, 10.63, 10.71, 10.62, 10.47);
        List<String> years = List.of("2019", "2020", "2021", "2022", "2023", "2024", "2025", "2026");

        // 简易线性回归外推（内置算法，不依赖外部 API）
        int n = history.size();
        double sx = 0, sy = 0, sxy = 0, sxx = 0;
        for (int i = 0; i < n; i++) {
            sx += i;
            sy += history.get(i);
            sxy += i * history.get(i);
            sxx += i * i;
        }
        double slope = (n * sxy - sx * sy) / (n * sxx - sx * sx);
        double intercept = (sy - slope * sx) / n;

        List<String> forecastYears = new ArrayList<>();
        List<Double> baseline = new ArrayList<>();
        List<Double> policy = new ArrayList<>();
        List<Double> upper = new ArrayList<>();
        List<Double> lower = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int t = n + i;
            double base = intercept + slope * t;
            forecastYears.add(String.valueOf(2027 + i));
            baseline.add(round(base, 2));
            // 政策情景：能源结构优化每期递减 2.1%
            policy.add(round(base * Math.pow(0.979, i + 1), 2));
            upper.add(round(base * 1.04, 2));
            lower.add(round(policy.get(i) * 0.97, 2));
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("years", years);
        m.put("history", history);
        m.put("forecastYears", forecastYears);
        m.put("baseline", baseline);
        m.put("policy", policy);
        m.put("upper", upper);
        m.put("lower", lower);
        m.put("unit", "亿吨 CO2e");
        m.put("model", "内生线性外推 + 政策情景（能源替代速率 -2.1%/年），演示数据");

        Map<String, Object> sectors = new LinkedHashMap<>();
        sectors.put("names", List.of("电力", "工业", "交通", "建筑", "农业", "其他"));
        sectors.put("values", List.of(38.6, 31.2, 10.4, 8.9, 6.3, 4.6));
        m.put("sectors", sectors);

        m.put("peakYear", "2024");
        m.put("peakValue", "10.71 亿吨");
        m.put("target2030", "较 2020 年下降 18%（目标值 ≈ 8.13 亿吨）");
        m.put("conclusion", List.of(
                "全国排放已于 2024 年见顶，进入平稳下行通道",
                "电力行业占比最高（38.6%），风光储替代是达峰后减排主引擎",
                "出口导向行业（钢铁/铝/水泥）面临 CBAM 碳关税成本上行压力"));
        return Result.ok(m);
    }

    /** 区域驾驶舱：各省市排放强度排行与升降趋势 */
    @GetMapping("/regional")
    public Result<Map<String, Object>> regional() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(region("贵州", "2.83", "1.721", "-6.2%", "down", "特色：磷化工与能源转型走廊"));
        rows.add(region("山东", "8.86", "1.898", "-3.1%", "down", "重点：煤电与钢铁行业深度脱碳"));
        rows.add(region("江苏", "8.02", "1.653", "-2.8%", "down", "重点：出口制造业绿色供应链"));
        rows.add(region("广东", "6.45", "1.204", "-4.6%", "down", "重点：外贸产品碳标签互认"));
        rows.add(region("内蒙古", "6.81", "2.616", "-1.2%", "down", "重点：煤电外送基地减碳"));
        rows.add(region("河北", "7.73", "2.105", "-2.4%", "down", "重点：钢铁行业超低排放"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rows", rows);
        m.put("updatedAt", java.time.LocalDateTime.now().toString());
        return Result.ok(m);
    }

    private double round(double v, int scale) {
        double f = Math.pow(10, scale);
        return Math.round(v * f) / f;
    }

    private Map<String, Object> region(String name, String total, String intensity, String trend, String dir, String note) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("total", total);
        m.put("intensity", intensity);
        m.put("trend", trend);
        m.put("dir", dir);
        m.put("note", note);
        return m;
    }
}