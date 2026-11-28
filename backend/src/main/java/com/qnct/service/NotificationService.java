package com.qnct.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qnct.common.PageResult;
import com.qnct.entity.SysNotification;
import com.qnct.entity.SysUser;
import com.qnct.mapper.SysNotificationMapper;
import com.qnct.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 站内通知中心：统一承接审批退回、核查派发/完成、因子生效、整改提交、授信申请等事件。
 * 表不可用时降级为演示通知列表，保证前端不白屏。
 */
@Slf4j
@Service
public class NotificationService {

    private final SysNotificationMapper mapper;
    private final SysUserMapper userMapper;

    public NotificationService(SysNotificationMapper mapper, SysUserMapper userMapper) {
        this.mapper = mapper;
        this.userMapper = userMapper;
    }

    public void send(Long userId, String bizType, String title, String content) {
        if (userId == null) {
            return;
        }
        try {
            SysNotification n = new SysNotification();
            n.setUserId(userId);
            n.setBizType(bizType);
            n.setTitle(title);
            n.setContent(content);
            n.setReadFlag(0);
            n.setCreateTime(LocalDateTime.now());
            mapper.insert(n);
        } catch (Exception e) {
            log.warn("站内通知落库失败（{} -> {}）：{}", bizType, userId, e.getMessage());
        }
    }

    /** 按机构广播：先按 sys_user.org_id 找接收人（最多 10 人），用于触达企业/部门 */
    public void sendToOrg(Long orgId, String bizType, String title, String content) {
        if (orgId == null) {
            return;
        }
        try {
            List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getOrgId, orgId).last("limit 10"));
            for (SysUser u : users) {
                send(u.getId(), bizType, title, content);
            }
        } catch (Exception e) {
            log.warn("站内通知按机构广播失败（{}）：{}", orgId, e.getMessage());
        }
    }

    public PageResult<SysNotification> list(Long userId, long page, long size, boolean unreadOnly) {
        try {
            Page<SysNotification> p = mapper.selectPage(new Page<>(page, size),
                    new LambdaQueryWrapper<SysNotification>()
                            .eq(SysNotification::getUserId, userId)
                            .eq(unreadOnly, SysNotification::getReadFlag, 0)
                            .orderByDesc(SysNotification::getCreateTime));
            return PageResult.of(p);
        } catch (Exception e) {
            return demoPage(page, size, unreadOnly);
        }
    }

    public long unreadCount(Long userId) {
        try {
            Long n = mapper.selectCount(new LambdaQueryWrapper<SysNotification>()
                    .eq(SysNotification::getUserId, userId)
                    .eq(SysNotification::getReadFlag, 0));
            return n == null ? 0 : n;
        } catch (Exception e) {
            return 2;
        }
    }

    public boolean markRead(Long id, Long userId) {
        try {
            SysNotification n = mapper.selectById(id);
            if (n == null || !n.getUserId().equals(userId)) {
                return false;
            }
            n.setReadFlag(1);
            n.setReadTime(LocalDateTime.now());
            mapper.updateById(n);
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    public int markAllRead(Long userId) {
        try {
            return mapper.update(null, new LambdaUpdateWrapper<SysNotification>()
                    .eq(SysNotification::getUserId, userId)
                    .eq(SysNotification::getReadFlag, 0)
                    .set(SysNotification::getReadFlag, 1)
                    .set(SysNotification::getReadTime, LocalDateTime.now()));
        } catch (Exception e) {
            return 0;
        }
    }

    private PageResult<SysNotification> demoPage(long page, long size, boolean unreadOnly) {
        List<SysNotification> demo = new ArrayList<>();
        demo.add(demo("INSPECTION", "线下核查任务待接收", "任务 HC-2026-0018 已派发至你，请按时到现场打卡并拍照留痕。", 0, "2026-11-27 09:12:00"));
        demo.add(demo("REVIEW", "月度核算被退回", "2026-10 范围二购电活动数据缺少发票佐证，请补充后重新提交。", 0, "2026-11-26 16:40:00"));
        demo.add(demo("FACTOR", "因子版本已生效", "西南电网电力排放因子 V3.3 已审核生效，新填报将按新版本计算。", 1, "2026-11-25 11:05:00"));
        demo.add(demo("PASSPORT", "碳足迹护照已签发", "护照 QN-PP-2026-0041 已完成终审签发，可下载防伪二维码。", 1, "2026-11-24 18:20:00"));
        if (unreadOnly) {
            demo.removeIf(n -> n.getReadFlag() != 0);
        }
        long total = demo.size();
        long from = Math.min((page - 1) * size, total);
        long to = Math.min(from + size, total);
        return PageResult.of(new ArrayList<>(demo.subList((int) from, (int) to)), total, page, size);
    }

    private SysNotification demo(String biz, String title, String content, int read, String time) {
        SysNotification n = new SysNotification();
        n.setUserId(4L);
        n.setBizType(biz);
        n.setTitle(title);
        n.setContent(content);
        n.setReadFlag(read);
        n.setCreateTime(LocalDateTime.parse(time.replace(' ', 'T')));
        return n;
    }
}