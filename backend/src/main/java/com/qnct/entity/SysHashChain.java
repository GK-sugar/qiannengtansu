package com.qnct.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_hash_chain")
public class SysHashChain {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long blockSeq;
    /** 前一区块哈希（创世块全 0） */
    private String prevHash;
    /** 本区块数据哈希 */
    private String dataHash;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String payloadJson;
    private Long operatorId;
    private LocalDateTime createTime;
}