package com.qnct.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 工具（JDK 自带实现，不引第三方；用于 F10 哈希链与 F11 防伪令牌）
 */
public final class Sha256Util {

    private Sha256Util() {}

    public static String hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : d) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }

    /** F10 区块指纹：h(i) = SHA-256(salt ‖ prevHash ‖ dataHash) */
    public static String blockHash(String prevHash, String dataHash, String salt) {
        return hex(salt + prevHash + dataHash);
    }
}