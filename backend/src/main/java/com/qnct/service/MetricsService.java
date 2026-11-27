package com.qnct.service;

import com.sun.management.OperatingSystemMXBean;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统运行与接口监控：JVM/OS 指标采集（供 admin/monitor 与监控看板）
 */
@Service
public class MetricsService {

    public Map<String, Object> snapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        Runtime rt = Runtime.getRuntime();
        long maxMem = rt.maxMemory();
        long usedMem = rt.totalMemory() - rt.freeMemory();
        double memPct = maxMem > 0 ? usedMem * 100.0 / maxMem : 0;

        double cpuPct = -1;
        long totalMem = maxMem, freeMem = maxMem - usedMem;
        try {
            OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            cpuPct = os.getSystemCpuLoad();
            totalMem = os.getTotalMemorySize();
            freeMem = os.getFreeMemorySize();
        } catch (Exception ignored) {
        }

        long diskTotal = 0, diskFree = 0;
        for (File root : File.listRoots()) {
            diskTotal += root.getTotalSpace();
            diskFree += root.getFreeSpace();
        }
        double diskPct = diskTotal > 0 ? (diskTotal - diskFree) * 100.0 / diskTotal : 0;

        m.put("cpu", cpuPct < 0 ? -1 : Math.round(cpuPct * 1000) / 10.0);
        m.put("memory", Math.round(memPct * 10) / 10.0);
        m.put("memoryUsedMb", usedMem / 1024 / 1024);
        m.put("systemMemUsedPct", totalMem > 0 ? Math.round((totalMem - freeMem) * 1000.0 / totalMem) / 10.0 : -1);
        m.put("disk", Math.round(diskPct * 10) / 10.0);
        m.put("diskUsedGb", (diskTotal - diskFree) / 1024 / 1024 / 1024);
        m.put("diskTotalGb", diskTotal / 1024 / 1024 / 1024);
        m.put("uptimeSec", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        m.put("jvm", System.getProperty("java.version"));
        m.put("processors", rt.availableProcessors());
        return m;
    }
}