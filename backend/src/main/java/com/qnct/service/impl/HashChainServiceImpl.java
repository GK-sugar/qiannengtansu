package com.qnct.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qnct.common.Constants;
import com.qnct.entity.SysHashChain;
import com.qnct.mapper.SysHashChainMapper;
import com.qnct.service.HashChainService;
import com.qnct.util.Sha256Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 哈希链实现（F10）：优先落库 sys_hash_chain；MySQL 不可用时自动降级内存链（演示模式）。
 *
 * 数据结构约定：
 *   dataHash    = SHA-256(salt ‖ prevHash ‖ payloadHash) —— 链上指纹（防篡改锚点）
 *   payloadHash = SHA-256(payloadJson ‖ salt)            —— 数据自身指纹
 * 篡改任何 payload 或任何一块，其后所有块 dataHash 均连锁失效（雪崩）。
 */
@Slf4j
@Service
public class HashChainServiceImpl implements HashChainService {

    @Value("${app.hash.salt:QNCT-SITE-SALT-2026}")
    private String salt;

    private final SysHashChainMapper mapper;
    private final ObjectMapper objectMapper;

    /** 内存降级链（数据库不可用时的演示模式） */
    private final List<SysHashChain> memory = new ArrayList<>();
    private final AtomicLong memSeq = new AtomicLong(94000);

    public HashChainServiceImpl(SysHashChainMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(new TreeMap<>(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("payload 序列化失败", e);
        }
    }

    private String payloadHash(String json) {
        return Sha256Util.hex(json + salt);
    }

    private String blockHash(String prevHash, String ph) {
        return Sha256Util.hex(salt + prevHash + ph);
    }

    @Override
    public SysHashChain append(String bizType, Long bizId, String bizNo, Map<String, Object> payload, Long operatorId) {
        String json = toJson(payload);
        String ph = payloadHash(json);
        try {
            SysHashChain prev = mapper.selectOne(new LambdaQueryWrapper<SysHashChain>()
                    .orderByDesc(SysHashChain::getBlockSeq).last("limit 1"));
            return buildBlock(prev == null ? null : prev.getBlockSeq(),
                    prev == null ? Constants.GENESIS_PREV_HASH : prev.getDataHash(),
                    bizType, bizId, bizNo, json, ph, operatorId, true);
        } catch (Exception e) {
            log.warn("哈希链落库不可用，切换内存演示链：{}", e.getMessage());
            SysHashChain tail = memory.isEmpty() ? null : memory.get(memory.size() - 1);
            long seq = tail == null ? memSeq.incrementAndGet() : tail.getBlockSeq() + 1;
            SysHashChain block = buildBlock(seq,
                    tail == null ? Constants.GENESIS_PREV_HASH : tail.getDataHash(),
                    bizType, bizId, bizNo, json, ph, operatorId, false);
            memory.add(block);
            return block;
        }
    }

    private SysHashChain buildBlock(Long seq, String prevHash, String bizType, Long bizId,
                                    String bizNo, String json, String ph, Long operatorId, boolean db) {
        SysHashChain b = new SysHashChain();
        b.setBlockSeq(seq == null ? 1 : seq + 1);
        b.setPrevHash(prevHash);
        b.setDataHash(blockHash(prevHash, ph));
        b.setBizType(bizType);
        b.setBizId(bizId);
        b.setBizNo(bizNo);
        b.setPayloadJson(json);
        b.setOperatorId(operatorId);
        b.setCreateTime(LocalDateTime.now());
        if (db) {
            mapper.insert(b);
        }
        return b;
    }

    @Override
    public List<SysHashChain> listByBiz(String bizType, Long bizId) {
        try {
            return mapper.selectList(new LambdaQueryWrapper<SysHashChain>()
                    .eq(SysHashChain::getBizType, bizType)
                    .eq(SysHashChain::getBizId, bizId)
                    .orderByAsc(SysHashChain::getBlockSeq));
        } catch (Exception e) {
            return memory.stream()
                    .filter(b -> bizType.equals(b.getBizType()) && bizId.equals(b.getBizId()))
                    .toList();
        }
    }

    @Override
    public List<HashVerifyResult> verifyAll() {
        List<SysHashChain> chain;
        try {
            chain = mapper.selectList(new LambdaQueryWrapper<SysHashChain>()
                    .orderByAsc(SysHashChain::getBlockSeq));
        } catch (Exception e) {
            chain = new ArrayList<>(memory);
        }
        List<HashVerifyResult> results = new ArrayList<>();
        String running = Constants.GENESIS_PREV_HASH;
        for (SysHashChain b : chain) {
            String recomputed = blockHash(running, payloadHash(b.getPayloadJson()));
            boolean ok = running.equals(b.getPrevHash()) && recomputed.equals(b.getDataHash());
            results.add(new HashVerifyResult(b.getBlockSeq(), b.getBizType(), b.getBizNo(),
                    b.getDataHash(), recomputed, ok));
            running = b.getDataHash();
        }
        return results;
    }

    @Override
    public boolean verifyPayload(String bizType, Long bizId, Map<String, Object> payload) {
        String target = payloadHash(toJson(payload));
        return listByBiz(bizType, bizId).stream()
                .anyMatch(b -> payloadHash(b.getPayloadJson()).equals(target));
    }

    @Override
    public String fingerprint(String bizType, String bizNo, Map<String, Object> payload) {
        return payloadHash(toJson(payload));
    }
}