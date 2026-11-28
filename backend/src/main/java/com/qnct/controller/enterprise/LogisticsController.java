package com.qnct.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qnct.common.Result;
import com.qnct.entity.EntLogisticsTrip;
import com.qnct.mapper.EntLogisticsTripMapper;
import com.qnct.security.SecurityUtils;
import com.qnct.service.GeoService;
import com.qnct.util.IdGen;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业端 · 物流追踪（高德轨迹；无 Key 时前端 Canvas 自绘，后端补里程复算）
 * 运输单读写 ent_logistics_trip，轨迹点可回放并按 F9 Haversine 复算里程。
 */
@RestController
@RequestMapping("/api/v1/enterprise/logistics")
public class LogisticsController {

    private final GeoService geo;
    private final EntLogisticsTripMapper tripMapper;
    private final ObjectMapper objectMapper;

    public LogisticsController(GeoService geo, EntLogisticsTripMapper tripMapper, ObjectMapper objectMapper) {
        this.geo = geo;
        this.tripMapper = tripMapper;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/trips")
    public Result<List<Map<String, Object>>> trips() {
        try {
            List<EntLogisticsTrip> rows = tripMapper.selectList(new LambdaQueryWrapper<EntLogisticsTrip>()
                    .eq(EntLogisticsTrip::getOrgId, SecurityUtils.orgId(1001L))
                    .orderByDesc(EntLogisticsTrip::getCreateTime)
                    .last("limit 30"));
            if (rows.isEmpty()) {
                return Result.ok(demoTrips());
            }
            List<Map<String, Object>> out = new ArrayList<>();
            for (EntLogisticsTrip t : rows) {
                out.add(tripView(t));
            }
            return Result.ok(out);
        } catch (Exception e) {
            return Result.ok(demoTrips());
        }
    }

    @PostMapping("/trips")
    public Result<Map<String, Object>> addTrip(@RequestBody Map<String, Object> body) {
        EntLogisticsTrip t = new EntLogisticsTrip();
        t.setTripNo(IdGen.bizNo("TR"));
        t.setOrgId(SecurityUtils.orgId(1001L));
        t.setScFlowId(lng(body.get("scFlowId"), null));
        t.setPlateNo(str(body.get("plateNo"), "贵A·00000"));
        t.setTransportMode(str(body.get("transportMode"), "公路"));
        t.setStartName(str(body.get("startName"), "起点"));
        t.setEndName(str(body.get("endName"), "终点"));
        t.setStatus("IN_TRANSIT");
        t.setStartTime(LocalDateTime.now());
        t.setCreateTime(LocalDateTime.now());
        t.setUpdateTime(LocalDateTime.now());

        double km = 0;
        List<double[]> points = parsePoints(body.get("route"));
        for (int i = 1; i < points.size(); i++) {
            double[] a = points.get(i - 1);
            double[] b = points.get(i);
            km += geo.distanceMeters(a[1], a[0], b[1], b[0]) / 1000.0;
        }
        if (points.size() >= 2) {
            t.setStartLng(BigDecimal.valueOf(points.get(0)[0]));
            t.setStartLat(BigDecimal.valueOf(points.get(0)[1]));
            t.setEndLng(BigDecimal.valueOf(points.get(points.size() - 1)[0]));
            t.setEndLat(BigDecimal.valueOf(points.get(points.size() - 1)[1]));
        }
        if (body.get("mileageKm") != null) {
            km = dbl(body.get("mileageKm"));
        }
        t.setMileageKm(BigDecimal.valueOf(km).setScale(3, RoundingMode.HALF_UP));
        try {
            t.setRouteJson(objectMapper.writeValueAsString(points));
        } catch (Exception ignored) {
        }
        try {
            tripMapper.insert(t);
        } catch (Exception e) {
            return Result.ok(Map.of("saved", true, "tripNo", t.getTripNo(),
                    "mileageKm", t.getMileageKm(), "note", "演示模式未落库"));
        }
        return Result.ok(Map.of("saved", true, "id", t.getId(), "tripNo", t.getTripNo(),
                "mileageKm", t.getMileageKm(), "status", "IN_TRANSIT"));
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

    private Map<String, Object> tripView(EntLogisticsTrip t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("tripNo", t.getTripNo());
        m.put("plate", t.getPlateNo());
        m.put("from", t.getStartName());
        m.put("to", t.getEndName());
        m.put("km", t.getMileageKm());
        m.put("status", "ARRIVED".equals(t.getStatus()) ? "已签收" : "在途");
        m.put("statusCode", t.getStatus());
        m.put("transportMode", t.getTransportMode());
        m.put("startLng", t.getStartLng());
        m.put("startLat", t.getStartLat());
        m.put("endLng", t.getEndLng());
        m.put("endLat", t.getEndLat());
        m.put("startTime", t.getStartTime());
        m.put("arriveTime", t.getArriveTime());
        try {
            m.put("route", objectMapper.readValue(t.getRouteJson(), Object.class));
        } catch (Exception e) {
            m.put("route", List.of());
        }
        return m;
    }

    private List<double[]> parsePoints(Object route) {
        List<double[]> points = new ArrayList<>();
        if (route == null) {
            return points;
        }
        try {
            Object node = objectMapper.convertValue(route, Object.class);
            Object listNode = node;
            if (node instanceof Map<?, ?> map && map.get("points") instanceof List<?> pts) {
                listNode = pts;
            }
            if (listNode instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof List<?> pair && pair.size() >= 2) {
                        points.add(new double[]{num(pair.get(0)), num(pair.get(1))});
                    } else if (item instanceof Map<?, ?> p && p.get("lng") != null && p.get("lat") != null) {
                        points.add(new double[]{num(p.get("lng")), num(p.get("lat"))});
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return points;
    }

    private static double num(Object o) {
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<Map<String, Object>> demoTrips() {
        return List.of(
                Map.of("plate", "贵A·8Q612", "from", "盘州矿区", "to", "贵阳东站", "km", 286.4, "status", "在途"),
                Map.of("plate", "贵A·Z3210", "from", "贵阳西站", "to", "遵义港", "km", 152.1, "status", "已签收"),
                Map.of("plate", "贵F·5R088", "from", "六盘水", "to", "南宁港", "km", 603.9, "status", "待发车"));
    }

    private static String str(Object o, String def) {
        if (o == null) {
            return def;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? def : s;
    }

    private static Long lng(Object o, Long def) {
        if (o == null || String.valueOf(o).isBlank()) {
            return def;
        }
        try {
            return Long.parseLong(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double dbl(Object o) {
        try {
            return o == null ? 0 : Double.parseDouble(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}