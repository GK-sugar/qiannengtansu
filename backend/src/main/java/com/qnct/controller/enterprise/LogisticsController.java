package com.qnct.controller.enterprise;

import com.qnct.common.Result;
import com.qnct.service.GeoService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业端 · 物流追踪（高德轨迹；无 Key 时前端 Canvas 自绘，后端补里程复算）
 * AMap Web服务【此处需填入API Key】申请：https://console.amap.com/
 */
@RestController
@RequestMapping("/api/v1/enterprise/logistics")
public class LogisticsController {

    private final GeoService geo;

    public LogisticsController(GeoService geo) {
        this.geo = geo;
    }

    @GetMapping("/trips")
    public Result<List<Map<String, Object>>> trips() {
        return Result.ok(List.of(
                Map.of("plate", "贵A·8Q612", "from", "盘州矿区", "to", "贵阳东站", "km", 286.4, "status", "在途"),
                Map.of("plate", "贵A·Z3210", "from", "贵阳西站", "to", "遵义港", "km", 152.1, "status", "已签收"),
                Map.of("plate", "贵F·5R088", "from", "六盘水", "to", "南宁港", "km", 603.9, "status", "待发车")));
    }

    /** 轨迹里程复算：给定轨迹点（lng,lat）串算总里程（F9 叠加） */
    @PostMapping("/mileage")
    public Result<Map<String, Object>> mileage(@RequestBody List<double[]> points) {
        double sum = 0;
        for (int i = 1; i < points.size(); i++) {
            double[] a = points.get(i - 1);
            double[] b = points.get(i);
            sum += geo.distanceMeters(a[0], a[1], b[0], b[1]);
        }
        return Result.ok(Map.of("points", points.size(), "distanceM", Math.round(sum), "distanceKm", Math.round(sum / 100) / 10.0));
    }

    /** 逆地理编码：GPS → 中文地址（需高德 Key） */
    @GetMapping("/geocode")
    public Result<Map<String, Object>> geocode(@RequestParam double lng, @RequestParam double lat) {
        String addr = geo.geocode(lng, lat);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("lng", lng);
        m.put("lat", lat);
        m.put("address", addr);
        m.put("needKey", addr == null);
        return Result.ok(m);
    }
}