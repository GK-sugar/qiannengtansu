package com.qnct.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qnct.common.BizException;
import com.qnct.common.PageResult;
import com.qnct.entity.GovCheckinRecord;
import com.qnct.entity.GovInspectionTask;
import com.qnct.event.BizNotificationEvent;
import com.qnct.mapper.GovCheckinRecordMapper;
import com.qnct.mapper.GovInspectionTaskMapper;
import com.qnct.security.SecurityUtils;
import com.qnct.util.IdGen;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 省厅线下核查任务状态机：
 *   PENDING 待接收 → DOING 进行中 → DONE 已完成
 *   PENDING / DOING → OVERDUE 已逾期（计划结束时间已过）
 *   PENDING / DOING → CANCELED 已取消
 * 打卡（GPS 判距 F9）会写入 gov_checkin_record，首次有效打卡自动推进为 DOING；
 * 状态迁移同步发布站内通知事件。
 */
@Slf4j
@Service
public class InspectionService {

    private static final Map<Long, String> ORG_NAMES = Map.of(
            1001L, "贵州黔能矿业有限责任公司",
            1002L, "中伟新材料股份有限公司",
            1003L, "贵州磷化集团有限责任公司",
            1004L, "遵义铝业股份有限公司",
            1005L, "首钢水城钢铁（集团）",
            1006L, "贵州西南水泥有限公司");

    private final GovInspectionTaskMapper taskMapper;
    private final GovCheckinRecordMapper checkinMapper;
    private final GeoService geo;
    private final StorageGateway storage;
    private final ApplicationEventPublisher events;

    public InspectionService(GovInspectionTaskMapper taskMapper, GovCheckinRecordMapper checkinMapper,
                             GeoService geo, StorageGateway storage, ApplicationEventPublisher events) {
        this.taskMapper = taskMapper;
        this.checkinMapper = checkinMapper;
        this.geo = geo;
        this.storage = storage;
        this.events = events;
    }

    public PageResult<Map<String, Object>> list(String status, long page, long size) {
        markOverdue();
        try {
            Page<GovInspectionTask> p = taskMapper.selectPage(new Page<>(page, size),
                    new LambdaQueryWrapper<GovInspectionTask>()
                            .eq(status != null && !status.isBlank(), GovInspectionTask::getStatus, status)
                            .orderByDesc(GovInspectionTask::getCreateTime));
            List<Map<String, Object>> records = new ArrayList<>();
            for (GovInspectionTask t : p.getRecords()) {
                records.add(view(t, checkinCount(t.getId())));
            }
            return PageResult.of(records, p.getTotal(), page, size);
        } catch (Exception e) {
            List<Map<String, Object>> demo = new ArrayList<>();
            demo.add(demoTask("HC-2026-0018", "出口精煤批次数据核查", 1001L, "数据核查", "刘工", "DOING", "2026-12-02", 210));
            demo.add(demoTask("HC-2026-0014", "购电发票与班次日志核对", 1004L, "现场执法", "王队", "PENDING", "2026-12-05", 240));
            demo.add(demoTask("HC-2026-0009", "第三方物流单据复核", 1003L, "飞检", "张队", "DONE", "2026-11-25", 180));
            return PageResult.of(demo, demo.size(), page, size);
        }
    }

    public Map<String, Object> detail(Long id) {
        GovInspectionTask t = require(id);
        Map<String, Object> out = view(t, checkinCount(id));
        try {
            out.put("checkins", checkinMapper.selectList(new LambdaQueryWrapper<GovCheckinRecord>()
                    .eq(GovCheckinRecord::getTaskId, id)
                    .orderByDesc(GovCheckinRecord::getCheckinTime)));
        } catch (Exception e) {
            out.put("checkins", List.of());
        }
        return out;
    }

    @Transactional
    public Map<String, Object> dispatch(Map<String, Object> body) {
        Long operator = SecurityUtils.userId(2L);
        GovInspectionTask t = new GovInspectionTask();
        t.setTaskNo(IdGen.bizNo("HC"));
        t.setTitle(str(body.get("title"), "出口产品碳数据核查"));
        t.setOrgId(lng(body.get("orgId"), 1001L));
        t.setType(str(body.get("type"), "数据核查"));
        t.setLeaderId(operator);
        t.setExecutorId(lng(body.get("executorId"), 5L));
        t.setPlanStart(time(body.get("planStart"), LocalDateTime.now()));
        t.setPlanEnd(time(body.get("planEnd"), LocalDateTime.now().plusDays(3)));
        t.setAddress(str(body.get("address"), "待补充核查地址"));
        t.setLng(dec(body.get("lng")));
        t.setLat(dec(body.get("lat")));
        t.setRadiusM(lng(body.get("radiusM"), 200L).intValue());
        t.setDesc(str(body.get("desc"), "核对活动数据、化验单与供应链凭证。"));
        t.setStatus("PENDING");
        t.setCreateTime(LocalDateTime.now());
        t.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(t);
        events.publishEvent(new BizNotificationEvent("INSPECTION", t.getExecutorId(), null,
                "线下核查任务待接收",
                "任务 " + t.getTaskNo() + "（" + t.getType() + "）已派发，请按时到达 " + t.getAddress() + " 打卡。"));
        return view(t, 0);
    }

    @Transactional
    public Map<String, Object> start(Long id) {
        GovInspectionTask t = require(id);
        if (!"PENDING".equals(t.getStatus())) {
            throw new BizException("仅待接收任务可开始执行，当前状态：" + t.getStatus());
        }
        t.setStatus("DOING");
        t.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(t);
        events.publishEvent(new BizNotificationEvent("INSPECTION", t.getLeaderId(), t.getOrgId(),
                "线下核查已开始", "任务 " + t.getTaskNo() + " 已由核查员接收并进入现场执行。"));
        return view(t, checkinCount(id));
    }

    @Transactional
    public Map<String, Object> checkin(Long taskId, Map<String, Object> body) {
        GovInspectionTask t = require(taskId);
        if ("DONE".equals(t.getStatus()) || "CANCELED".equals(t.getStatus())) {
            throw new BizException("任务已结束，不能继续打卡");
        }
        Long userId = SecurityUtils.userId(t.getExecutorId());
        double lat = dbl(body.get("lat"));
        double lng = dbl(body.get("lng"));
        double accuracy = dbl(body.get("accuracy"));
        double radius = t.getRadiusM() == null ? 200 : t.getRadiusM();

        double distance = 0;
        boolean hasTarget = t.getLng() != null && t.getLat() != null;
        if (hasTarget && lat != 0 && lng != 0) {
            distance = geo.distanceMeters(lat, lng, t.getLat().doubleValue(), t.getLng().doubleValue());
        }
        String status = "VALID";
        if (hasTarget && distance > radius) {
            status = "OFFSITE";
        } else if (accuracy > 500) {
            status = "ABNORMAL";
        }

        GovCheckinRecord r = new GovCheckinRecord();
        r.setTaskId(taskId);
        r.setUserId(userId);
        r.setCheckinTime(LocalDateTime.now());
        r.setLng(BigDecimal.valueOf(lng).setScale(7, RoundingMode.HALF_UP));
        r.setLat(BigDecimal.valueOf(lat).setScale(7, RoundingMode.HALF_UP));
        r.setAccuracy(accuracy == 0 ? null : BigDecimal.valueOf(accuracy));
        String address = str(body.get("address"), null);
        if (address == null) {
            address = geo.geocode(lng, lat);
        }
        r.setAddress(address);
        r.setDistanceM(BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP));
        r.setDeviceInfo(str(body.get("deviceInfo"), "H5 GPS"));
        r.setStatus(status);
        r.setCreateTime(LocalDateTime.now());
        String photo = str(body.get("photo"), "");
        if (!photo.isBlank()) {
            r.setPhotoPath(storage.saveBase64("checkin", photo));
        }
        checkinMapper.insert(r);

        if ("PENDING".equals(t.getStatus()) && "VALID".equals(status)) {
            t.setStatus("DOING");
            t.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(t);
        }
        if (!"VALID".equals(status)) {
            events.publishEvent(new BizNotificationEvent("INSPECTION", t.getLeaderId(), null,
                    "打卡异常提醒", "任务 " + t.getTaskNo() + " 本次打卡判定为 " + status
                            + "（距目标 " + Math.round(distance) + "m / 半径 " + (int) radius + "m）。"));
        }
        return recordView(r);
    }

    @Transactional
    public Map<String, Object> complete(Long id, Map<String, Object> body) {
        GovInspectionTask t = require(id);
        if (!"DOING".equals(t.getStatus()) && !"OVERDUE".equals(t.getStatus())) {
            throw new BizException("仅进行中/逾期的任务可完成，当前状态：" + t.getStatus());
        }
        boolean force = Boolean.parseBoolean(str(body.get("force"), "false"));
        long valid = countValidCheckin(id);
        if (valid == 0 && !force) {
            throw new BizException("尚无有效打卡记录，无法完成归档（如需强制，传 force=true）");
        }
        t.setStatus("DONE");
        t.setFinishTime(LocalDateTime.now());
        t.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(t);
        events.publishEvent(new BizNotificationEvent("INSPECTION", t.getLeaderId(), t.getOrgId(),
                "线下核查已完成", "任务 " + t.getTaskNo() + " 已完成，有效打卡 " + valid + " 次，结果进入复核归档。"));
        return view(t, checkinCount(id));
    }

    @Transactional
    public Map<String, Object> cancel(Long id, String reason) {
        GovInspectionTask t = require(id);
        if ("DONE".equals(t.getStatus()) || "CANCELED".equals(t.getStatus())) {
            throw new BizException("任务已结束，不能取消");
        }
        t.setStatus("CANCELED");
        t.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(t);
        events.publishEvent(new BizNotificationEvent("INSPECTION", t.getExecutorId(), t.getOrgId(),
                "线下核查已取消", "任务 " + t.getTaskNo() + " 已取消。原因：" + (reason == null ? "未填写" : reason)));
        return view(t, checkinCount(id));
    }

    /** 归档：仅允许已完成任务，回执附打卡明细供审计 */
    public Map<String, Object> archive(Long id) {
        GovInspectionTask t = require(id);
        if (!"DONE".equals(t.getStatus())) {
            throw new BizException("仅已完成任务可归档，当前状态：" + t.getStatus());
        }
        Map<String, Object> out = detail(id);
        out.put("archived", true);
        out.put("archiveTime", LocalDateTime.now());
        return out;
    }

    /** 定时将超期未完成任务置为 OVERDUE */
    @Scheduled(fixedDelay = 600_000, initialDelay = 30_000)
    public void sweepOverdue() {
        try {
            int n = taskMapper.update(null, new LambdaUpdateWrapper<GovInspectionTask>()
                    .in(GovInspectionTask::getStatus, List.of("PENDING", "DOING"))
                    .lt(GovInspectionTask::getPlanEnd, LocalDateTime.now())
                    .set(GovInspectionTask::getStatus, "OVERDUE")
                    .set(GovInspectionTask::getUpdateTime, LocalDateTime.now()));
            if (n > 0) {
                log.info("[核查任务] 逾期标记 {} 条", n);
            }
        } catch (Exception e) {
            log.debug("核查逾期巡检跳过：{}", e.getMessage());
        }
    }

    private void markOverdue() {
        try {
            taskMapper.update(null, new LambdaUpdateWrapper<GovInspectionTask>()
                    .in(GovInspectionTask::getStatus, List.of("PENDING", "DOING"))
                    .lt(GovInspectionTask::getPlanEnd, LocalDateTime.now())
                    .set(GovInspectionTask::getStatus, "OVERDUE")
                    .set(GovInspectionTask::getUpdateTime, LocalDateTime.now()));
        } catch (Exception ignored) {
        }
    }

    private GovInspectionTask require(Long id) {
        GovInspectionTask t;
        try {
            t = taskMapper.selectById(id);
        } catch (Exception e) {
            throw new BizException("核查任务不可用：" + e.getMessage());
        }
        if (t == null) {
            throw new BizException("核查任务不存在：" + id);
        }
        return t;
    }

    private long checkinCount(Long taskId) {
        try {
            Long n = checkinMapper.selectCount(new LambdaQueryWrapper<GovCheckinRecord>()
                    .eq(GovCheckinRecord::getTaskId, taskId));
            return n == null ? 0 : n;
        } catch (Exception e) {
            return 0;
        }
    }

    private long countValidCheckin(Long taskId) {
        try {
            Long n = checkinMapper.selectCount(new LambdaQueryWrapper<GovCheckinRecord>()
                    .eq(GovCheckinRecord::getTaskId, taskId)
                    .eq(GovCheckinRecord::getStatus, "VALID"));
            return n == null ? 0 : n;
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<String, Object> view(GovInspectionTask t, long checkins) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("taskId", t.getTaskNo());
        m.put("title", t.getTitle());
        m.put("orgId", t.getOrgId());
        m.put("org", ORG_NAMES.getOrDefault(t.getOrgId(), "企业 " + t.getOrgId()));
        m.put("type", t.getType());
        m.put("leaderId", t.getLeaderId());
        m.put("executorId", t.getExecutorId());
        m.put("planStart", t.getPlanStart());
        m.put("planEnd", t.getPlanEnd());
        m.put("deadline", t.getPlanEnd() == null ? "" : t.getPlanEnd().toLocalDate().toString());
        m.put("address", t.getAddress());
        m.put("lng", t.getLng());
        m.put("lat", t.getLat());
        m.put("radiusM", t.getRadiusM());
        m.put("range", (t.getRadiusM() == null ? 0 : t.getRadiusM()) + "m");
        m.put("desc", t.getDesc());
        m.put("status", t.getStatus());
        m.put("finishTime", t.getFinishTime());
        m.put("checkinCount", checkins);
        return m;
    }

    private Map<String, Object> recordView(GovCheckinRecord r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("taskId", r.getTaskId());
        m.put("userId", r.getUserId());
        m.put("checkinTime", r.getCheckinTime());
        m.put("lng", r.getLng());
        m.put("lat", r.getLat());
        m.put("accuracy", r.getAccuracy());
        m.put("address", r.getAddress());
        m.put("distanceM", r.getDistanceM());
        m.put("status", r.getStatus());
        m.put("photoPath", r.getPhotoPath());
        m.put("pass", "VALID".equals(r.getStatus()));
        return m;
    }

    private Map<String, Object> demoTask(String no, String title, Long orgId, String type, String executor,
                                         String status, String deadline, int radius) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("taskId", no);
        m.put("title", title);
        m.put("orgId", orgId);
        m.put("org", ORG_NAMES.getOrDefault(orgId, "企业 " + orgId));
        m.put("type", type);
        m.put("executor", executor);
        m.put("status", status);
        m.put("deadline", deadline);
        m.put("range", radius + "m");
        m.put("checkinCount", "DONE".equals(status) ? 2 : 0);
        return m;
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

    private static BigDecimal dec(Object o) {
        try {
            return o == null || String.valueOf(o).isBlank() ? null : new BigDecimal(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double dbl(Object o) {
        try {
            return o == null ? 0 : Double.parseDouble(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static LocalDateTime time(Object o, LocalDateTime def) {
        if (o == null || String.valueOf(o).isBlank()) {
            return def;
        }
        String s = String.valueOf(o).trim().replace(' ', 'T');
        try {
            if (s.length() == 10) {
                return LocalDate.parse(s).atTime(9, 0);
            }
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException e) {
            return def;
        }
    }
}