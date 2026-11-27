package com.qnct.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 业务编号生成器：前缀 + 日期 + 两位随机 + 三位序列
 */
public final class IdGen {

    private IdGen() {}

    public static String bizNo(String prefix) {
        int n = ThreadLocalRandom.current().nextInt(100, 999);
        return prefix + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%03d", n);
    }

    /** 护照编号：QN-PP-2026-0041 风格 */
    public static String passportNo() {
        return "QN-PP-" + LocalDate.now().getYear() + "-"
                + String.format("%04d", ThreadLocalRandom.current().nextInt(1, 9999));
    }
}