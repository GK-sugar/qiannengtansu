package com.qnct.security;

import com.qnct.common.Constants;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 演示模式用户加载：四端演示账号内置（正式环境切 sys_user 表 + BCrypt）。
 *   superadmin/admin888 · gov/gov888 · agency/agency888 · enterprise/ent888 · national/nat888（国家级审批部门）
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Map<String, LoginUser> DEMO = new HashMap<>();

    static {
        put(1L, "superadmin", "系统管理员", Constants.ROLE_ADMIN, Constants.AUTH_ADMIN,
                0L, "黔能碳溯平台", "ALL");
        put(2L, "gov", "李处长", Constants.ROLE_GOV, Constants.AUTH_GOV,
                1L, "贵州省生态环境厅", "REGION");
        put(3L, "agency", "陈主任", Constants.ROLE_AGENCY, Constants.AUTH_AGENCY,
                2L, "中环低碳国际核查中心", "ORG");
        put(4L, "enterprise", "王总", Constants.ROLE_ENTERPRISE, Constants.AUTH_ENTERPRISE,
                1001L, "贵州黔能矿业有限责任公司", "ORG");
        put(5L, "gov2", "张副处", Constants.ROLE_GOV, Constants.AUTH_GOV,
                1L, "贵州省生态环境厅", "REGION");
        put(6L, "ent2", "赵厂", Constants.ROLE_ENTERPRISE, Constants.AUTH_ENTERPRISE,
                1002L, "贵州中伟新能源材料有限公司", "ORG");
        put(7L, "national", "周司长", Constants.ROLE_NATIONAL, Constants.AUTH_NATIONAL,
                999L, "国家应对气候变化战略中心", "ALL");
    }

    private static void put(Long id, String username, String realName, String roleKey,
                            String authority, Long orgId, String orgName, String dataScope) {
        DEMO.put(username, new LoginUser(id, username, realName, roleKey, orgId, orgName, authority, dataScope));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LoginUser u = DEMO.get(username);
        if (u == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }
        return u;
    }

    /** 管理员重置的密码覆盖表（优先于内置口令；正式环境落 sys_user + BCrypt） */
    private static final Map<String, String> PWD_OVERRIDES = new ConcurrentHashMap<>();

    public void resetPassword(String username, String raw) {
        if (!DEMO.containsKey(username)) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }
        PWD_OVERRIDES.put(username, raw);
    }

    /** 演示口令校验（正式环境统一 BCrypt） */
    public boolean checkPassword(String username, String raw) {
        String override = PWD_OVERRIDES.get(username);
        if (override != null) {
            return override.equals(raw);
        }
        return switch (username) {
            case "superadmin" -> Constants.DEV_ADMIN_PWD.equals(raw);
            case "gov", "gov2" -> Constants.DEV_GOV_PWD.equals(raw);
            case "agency" -> Constants.DEV_AGENCY_PWD.equals(raw);
            case "enterprise", "ent2" -> Constants.DEV_ENT_PWD.equals(raw);
            case "national" -> Constants.DEV_NATIONAL_PWD.equals(raw);
            default -> false;
        };
    }

    public static boolean isDemoUser(String username) {
        return DEMO.containsKey(username);
    }
}