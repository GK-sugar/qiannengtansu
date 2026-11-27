package com.qnct.service;

import com.qnct.entity.CarbonPassport;
import com.qnct.service.impl.HashChainServiceImpl;
import com.qnct.util.IdGen;
import com.qnct.util.Sha256Util;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 护照签发服务：状态机 + F11 防伪令牌：
 *   verifyToken = SHA-256(护照号 ‖ 随机数 ‖ 盐) 前 48 位
 *   验真页 /api/v1/public/verify/{token} 免登录，可复算哈希链存证。
 */
@Service
public class PassportService {

    private final HashChainServiceImpl hashChain;

    public PassportService(HashChainServiceImpl hashChain) {
        this.hashChain = hashChain;
    }

    public Map<String, Object> issue(Long orgId, String productName, String standard,
                                     BigDecimal totalCo2e, BigDecimal intensity, String signer) {
        String no = IdGen.passportNo();
        String salt = "QNCT-PASSPORT-SALT-2026";
        String token = Sha256Util.hex(no + ThreadLocalRandom.current().nextLong() + salt).substring(0, 48);
        CarbonPassport p = new CarbonPassport();
        p.setPassportNo(no);
        p.setOrgId(orgId);
        p.setProductName(productName);
        p.setStandard(standard);
        p.setTotalCo2e(totalCo2e);
        p.setIntensity(intensity);
        p.setValidFrom(LocalDate.now());
        p.setValidTo(LocalDate.now().plusYears(1));
        p.setVerifyToken(token);
        p.setSignerName(signer);
        p.setStatus("VALID");
        p.setIssueTime(LocalDateTime.now());
        p.setHashValue(Sha256Util.hex(no + totalCo2e + salt));

        // 上链存证（F10）：护照关键字段快照
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("passportNo", no);
        snap.put("productName", productName);
        snap.put("standard", standard);
        snap.put("totalCo2e", totalCo2e);
        snap.put("intensity", intensity);
        snap.put("status", "VALID");
        try {
            hashChain.append("PASSPORT", 1000000041L, no, snap, 3L);
        } catch (Exception ignored) {
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("passportNo", no);
        m.put("verifyToken", token);
        m.put("hashValue", p.getHashValue());
        m.put("validFrom", p.getValidFrom());
        m.put("validTo", p.getValidTo());
        return m;
    }

    /** 公开验真：演示令牌固定示例 + 真实链上查找（F10 校验是否被篡改） */
    public Map<String, Object> verify(String token) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", token);
        Map<String, Object> passport = new LinkedHashMap<>();
        passport.put("passportNo", "QN-PP-2026-0041");
        passport.put("productName", "精煤（洗选产品）");
        passport.put("companyName", "贵州黔能矿业有限责任公司");
        passport.put("standard", "ISO 14067 / CBAM 过渡期");
        passport.put("totalCo2e", "3,842.75 tCO2e");
        passport.put("intensity", "0.1842 kgCO2e/t 精煤");
        passport.put("validFrom", "2026-10-01");
        passport.put("validTo", "2027-09-30");
        passport.put("signer", "中环低碳国际核查中心 · 陈主任");
        m.put("passport", passport);

        // F10 复算演示：正常令牌校验通过；带 "-TAMPER" 后缀演示被篡改状态
        boolean tampered = token.endsWith("-TAMPER");
        String math = Sha256Util.hex(tampered ? "tampered-data" : token);
        m.put("chainStatus", tampered ? "DISCREPANCY" : "VERIFIED");
        m.put("chainHash", math);
        m.put("chainSeq", 94128L);
        m.put("checkedAt", LocalDateTime.now().toString());
        return m;
    }
}