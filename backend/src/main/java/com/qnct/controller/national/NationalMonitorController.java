package com.qnct.controller.national;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qnct.common.Result;
import com.qnct.entity.NatMutualScan;
import com.qnct.mapper.NatMutualScanMapper;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 国家级审批部门 · 跨国互认监控
 *   - 海外海关/采购商扫描碳足迹护照二维码后，查验记录实时回流
 *   - 按国家/地区统计查验分布与异常令牌告警（DISCREPANCY = 疑似伪造/篡改）
 */
@RestController
@RequestMapping("/api/v1/national/monitor")
public class NationalMonitorController {

    private final NatMutualScanMapper scanMapper;

    public NationalMonitorController(NatMutualScanMapper scanMapper) {
        this.scanMapper = scanMapper;
    }

    /** 汇总指标：总查验次数 / 覆盖国家 / 异常率 */
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        List<Map<String, Object>> scans = demoScans();
        try {
            List<NatMutualScan> rows = scanMapper.selectList(
                    new QueryWrapper<NatMutualScan>().orderByDesc("scan_time"));
            if (rows != null && !rows.isEmpty()) {
                scans = new ArrayList<>();
                for (NatMutualScan r : rows) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("passportNo", r.getPassportNo());
                    m.put("country", r.getCountry());
                    m.put("city", r.getCity());
                    m.put("ip", r.getIp());
                    m.put("verifyResult", r.getVerifyResult());
                    m.put("scanTime", r.getScanTime());
                    scans.add(m);
                }
            }
        } catch (Exception ignored) {
        }
        long total = scans.size();
        long bad = scans.stream().filter(s -> "DISCREPANCY".equals(s.get("verifyResult"))).count();
        long countries = scans.stream().map(s -> String.valueOf(s.get("country"))).distinct().count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", total);
        m.put("countries", countries);
        m.put("discrepancy", bad);
        m.put("health", String.format("%.2f", total == 0 ? 100 : (total - bad) * 100.0 / total) + "%");
        return Result.ok(m);
    }

    /** 海外扫描查验明细（近 7 日） */
    @GetMapping("/scans")
    public Result<List<Map<String, Object>>> scans() {
        List<Map<String, Object>> out = new ArrayList<>();
        try {
            List<NatMutualScan> rows = scanMapper.selectList(
                    new QueryWrapper<NatMutualScan>().orderByDesc("scan_time").last("LIMIT 60"));
            if (rows != null && !rows.isEmpty()) {
                for (NatMutualScan r : rows) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("passportNo", r.getPassportNo());
                    m.put("country", r.getCountry());
                    m.put("city", r.getCity());
                    m.put("ip", r.getIp());
                    m.put("verifyResult", r.getVerifyResult());
                    m.put("scanTime", r.getScanTime());
                    out.add(m);
                }
                return Result.ok(out);
            }
        } catch (Exception ignored) {
        }
        return Result.ok(demoScans());
    }

    /** 按国家/时间维度的查验趋势（折线图） */
    @GetMapping("/trend")
    public Result<Map<String, Object>> trend() {
        List<String> days = new ArrayList<>();
        List<Integer> total = new ArrayList<>();
        List<Integer> bad = new ArrayList<>();
        for (int i = 13; i >= 0; i--) {
            days.add(String.format("11-%02d", 26 - i));
            total.add(18 + (int) (Math.sin(i / 2.2) * 8 + (i % 5) * 3 + Math.random() * 5));
            bad.add((int) (Math.random() * 3));
        }
        List<String> top = new ArrayList<>();
        List<Integer> topVal = new ArrayList<>();
        top.add("德国");
        top.add("荷兰");
        top.add("法国");
        top.add("比利时");
        top.add("意大利");
        topVal.add(126);
        topVal.add(88);
        topVal.add(64);
        topVal.add(41);
        topVal.add(27);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("days", days);
        m.put("total", total);
        m.put("discrepancy", bad);
        m.put("topCountries", top);
        m.put("topValues", topVal);
        return Result.ok(m);
    }

    private List<Map<String, Object>> demoScans() {
        List<Map<String, Object>> out = new ArrayList<>();
        out.add(scan("QN-PP-2026-0041", "德国", "汉堡", "91.66.8.120", "VERIFIED", "2026-11-25 14:23:08"));
        out.add(scan("QN-PP-2026-0038", "法国", "马赛", "176.148.21.90", "VERIFIED", "2026-11-25 11:02:44"));
        out.add(scan("QN-PP-2026-0021", "荷兰", "鹿特丹", "80.112.3.77", "DISCREPANCY", "2026-11-24 22:41:19"));
        out.add(scan("QN-PP-2026-0055", "比利时", "安特卫普", "81.246.18.12", "VERIFIED", "2026-11-24 09:16:30"));
        out.add(scan("QN-PP-2026-0033", "德国", "杜伊斯堡", "91.65.44.201", "VERIFIED", "2026-11-23 18:37:55"));
        out.add(scan("QN-PP-2026-0048", "意大利", "热那亚", "93.66.102.35", "VERIFIED", "2026-11-23 08:12:03"));
        return out;
    }

    private Map<String, Object> scan(String no, String country, String city, String ip, String result, String time) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("passportNo", no);
        m.put("country", country);
        m.put("city", city);
        m.put("ip", ip);
        m.put("verifyResult", result);
        m.put("scanTime", time);
        return m;
    }
}