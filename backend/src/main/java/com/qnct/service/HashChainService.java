package com.qnct.service;

import com.qnct.entity.SysHashChain;

import java.util.List;
import java.util.Map;

/**
 * F10 哈希链服务：存证上链 + 全量验链。
 * h(i) = SHA-256(salt ‖ h(i−1) ‖ SHA-256(data_i ‖ salt))
 * 任何一块发生篡改，后续所有块校验必失败（雪崩效应）。
 */
public interface HashChainService {

    /** 上链一条业务存证，返回区块 */
    SysHashChain append(String bizType, Long bizId, String bizNo, Map<String, Object> payload, Long operatorId);

    /** 按业务查询链 */
    List<SysHashChain> listByBiz(String bizType, Long bizId);

    /** 全量验链：逐块重算指纹并比对 */
    List<HashVerifyResult> verifyAll();

    /** 校验单条 payload 与链上 dataHash 是否一致（用于「快照现算指纹」） */
    boolean verifyPayload(String bizType, Long bizId, Map<String, Object> payload);

    /** 根据现拼 payload 计算指纹（前端一键校验时后端重算） */
    String fingerprint(String bizType, String bizNo, Map<String, Object> payload);

    /** 验链结果视图 */
    record HashVerifyResult(Long blockSeq, String bizType, String bizNo, String chainHash,
                            String nowHash, boolean ok) {}
}