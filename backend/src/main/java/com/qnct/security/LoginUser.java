package com.qnct.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 当前登录用户载体（JWT 解码后装配进 SecurityContext）
 */
@Getter
@AllArgsConstructor
public class LoginUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String realName;
    private final String roleKey;      // admin/gov/agency/enterprise（与前端同构）
    private final Long orgId;
    private final String orgName;
    private final String authority;    // ROLE_SUPER_ADMIN 等
    private final String dataScope;    // ALL / REGION / ORG / SELF（省-市-企业三级数据隔离）

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of((GrantedAuthority) () -> authority);
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}