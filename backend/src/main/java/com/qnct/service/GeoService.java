package com.qnct.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 位置服务：F9 Haversine 判距 + 高德逆地理编码（Web服务）。
 * 未填 AMAP_WEB_KEY 时 geocode 返回 null，前端仅展示经纬度。
 */
@Service
public class GeoService {

    @Value("${app.amap.web-key:}")
    private String amapKey;

    /** F9：Haversine 球面距离（米） */
    public double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    /** 打卡判定：距离小于允许半径即通过 */
    public boolean withinRadius(double lat1, double lng1, double lat2, double lng2, double radiusMeters) {
        return distanceMeters(lat1, lng1, lat2, lng2) <= radiusMeters;
    }

    /**
     * 高德逆地理编码（Web 服务 API）【此处需填入API Key】
     * 申请：https://console.amap.com/ → 应用管理 → 添加 Key → Web服务
     */
    public String geocode(double lng, double lat) {
        if (amapKey == null || amapKey.isBlank()) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            var map = (java.util.Map<String, Object>) RestClient.create("https://restapi.amap.com")
                    .get().uri("/v3/geocode/regeo?key=" + amapKey
                            + "&location=" + lng + "," + lat + "&extensions=base")
                    .retrieve().body(java.util.Map.class);
            if (map == null || !"1".equals(String.valueOf(map.get("status")))) {
                return null;
            }
            var regeo = (java.util.Map<String, Object>) map.get("regeocode");
            return regeo == null ? null : String.valueOf(regeo.get("formatted_address"));
        } catch (Exception e) {
            return null;
        }
    }
}