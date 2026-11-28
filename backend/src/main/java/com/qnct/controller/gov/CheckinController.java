package com.qnct.controller.gov;

import com.qnct.common.Result;
import com.qnct.service.GeoService;
import com.qnct.service.InspectionService;
import com.qnct.service.StorageGateway;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 省厅端 · 移动执法 H5 打卡（F9 GPS 判距 + 现场拍照留痕）
 * 携带 taskId 时走核查任务状态机并落 gov_checkin_record；否则保留自由打卡计算。
 */
@RestController
@RequestMapping("/api/v1/gov/checkin")
public class CheckinController {

    private final GeoService geo;
    private final StorageGateway storage;
    private final InspectionService inspection;

    public CheckinController(GeoService geo, StorageGateway storage, InspectionService inspection) {
        this.geo = geo;
        this.storage = storage;
        this.inspection = inspection;
    }

    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestBody Map<String, Object> body) {
        Object taskId = body.get("taskId");
        if (taskId != null && !String.valueOf(taskId).isBlank()) {
            return Result.ok(inspection.checkin(Long.parseLong(String.valueOf(taskId)), body));
        }
        double lat = Double.parseDouble(String.valueOf(body.getOrDefault("lat", "0")));
        double lng = Double.parseDouble(String.valueOf(body.getOrDefault("lng", "0")));
        double orgLat = Double.parseDouble(String.valueOf(body.getOrDefault("orgLat", "0")));
        double orgLng = Double.parseDouble(String.valueOf(body.getOrDefault("orgLng", "0")));
        double radius = Double.parseDouble(String.valueOf(body.getOrDefault("radius", "1500")));
        String photo = String.valueOf(body.getOrDefault("photo", ""));

        double dist = geo.distanceMeters(lat, lng, orgLat, orgLng);
        boolean pass = dist <= radius;
        String photoPath = photo.isBlank() ? null : storage.saveBase64("checkin", photo);
        String address = geo.geocode(lng, lat);
        return Result.ok(Map.of("pass", pass, "distanceM", Math.round(dist), "radiusM", radius,
                "address", address == null ? "" : address, "photoPath", photoPath == null ? "" : photoPath,
                "formula", "F9 Haversine：d = 2R·asin(√(sin²Δφ/2 + cosφ1·cosφ2·sin²Δλ/2))"));
    }
}