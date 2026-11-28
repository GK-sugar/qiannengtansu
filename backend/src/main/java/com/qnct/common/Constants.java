package com.qnct.common;

import java.util.List;

/**
 * 全局常量：角色、业务字典码、F10 哈希链盐值等
 */
public final class Constants {

    private Constants() {}

    /** 演示账号口令（开发期固定；生产必须迁移到 sys_user + BCrypt） */
    public static final String DEV_ADMIN_PWD = "admin888";
    public static final String DEV_GOV_PWD = "gov888";
    public static final String DEV_AGENCY_PWD = "agency888";
    public static final String DEV_ENT_PWD = "ent888";
    public static final String DEV_NATIONAL_PWD = "nat888";

    /** 角色键（前端 auth store 同构） */
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_GOV = "gov";
    public static final String ROLE_AGENCY = "agency";
    public static final String ROLE_ENTERPRISE = "enterprise";
    /** 国家级审批部门（终审发证 / 跨国互认 / 宏观监管） */
    public static final String ROLE_NATIONAL = "national";

    /** Spring Security 权限 */
    public static final String AUTH_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String AUTH_GOV = "ROLE_GOV";
    public static final String AUTH_AGENCY = "ROLE_AGENCY";
    public static final String AUTH_ENTERPRISE = "ROLE_ENTERPRISE";
    public static final String AUTH_NATIONAL = "ROLE_NATIONAL";

    /** F10 哈希链创世块前一哈希（全 0） */
    public static final String GENESIS_PREV_HASH = "0".repeat(64);

    /** 哈希链业务类型 */
    public static final List<String> HASH_BIZ_TYPES =
            List.of("CARBON_RESULT", "CARBON_REPORT", "FACTOR", "PASSPORT", "PACKAGE", "RECTIFY");

    /** 波动预警默认阈值（F6） */
    public static final double DEFAULT_FLUCTUATION_THRESHOLD = 0.20;

    /** 数据字典编码 */
    public static final String DICT_INDUSTRY = "dict_industry";
    public static final String DICT_MATERIAL = "dict_material";
    public static final String DICT_REGION = "dict_region";
    public static final String DICT_EMISSION_SOURCE = "dict_emission_source";
}