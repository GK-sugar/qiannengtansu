package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sc_collab_task")
public class ScCollabTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orgId;
    private Long partnerId;
    /** DATA_COLLECT / CONFIRM / FEEDBACK */
    private String taskType;
    private String title;
    private String content;
    private LocalDateTime deadline;
    /** PENDING / DOING / DONE / OVERDUE */
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}