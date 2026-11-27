package com.qnct.controller;

import com.qnct.common.Result;
import com.qnct.dto.LoginRequest;
import com.qnct.dto.LoginResponse;
import com.qnct.dto.UserInfoDto;
import com.qnct.security.JwtUtil;
import com.qnct.security.LoginUser;
import com.qnct.security.UserDetailsServiceImpl;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证：登录/登出/当前用户（验证码留接口，演示模式后端固定四位码）
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserDetailsServiceImpl users;
    private final JwtUtil jwtUtil;

    public AuthController(UserDetailsServiceImpl users, JwtUtil jwtUtil) {
        this.users = users;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginUser u;
        try {
            u = (LoginUser) users.loadUserByUsername(req.username());
        } catch (Exception e) {
            return Result.fail(401, "账号或密码错误");
        }
        if (!users.checkPassword(req.username(), req.password())) {
            return Result.fail(401, "账号或密码错误");
        }
        String token = jwtUtil.issue(u.getId(), u.getUsername(), Map.of(
                "realName", u.getRealName(),
                "roleKey", u.getRoleKey(),
                "orgId", u.getOrgId(),
                "orgName", u.getOrgName(),
                "authority", u.getAuthority(),
                "dataScope", u.getDataScope()));
        UserInfoDto info = new UserInfoDto(u.getId(), u.getUsername(), u.getRealName(), u.getRoleKey(),
                u.getOrgId(), u.getOrgName(), u.getDataScope(), homePath(u.getRoleKey()));
        return Result.ok(new LoginResponse(token, info));
    }

    private String homePath(String roleKey) {
        return switch (roleKey) {
            case "admin" -> "/admin/dashboard";
            case "gov" -> "/gov/dashboard";
            case "agency" -> "/agency/dashboard";
            case "national" -> "/national/issuance";
            default -> "/enterprise/dashboard";
        };
    }

    @GetMapping("/me")
    public Result<UserInfoDto> me(Authentication auth) {
        LoginUser u = (LoginUser) auth.getPrincipal();
        return Result.ok(new UserInfoDto(u.getId(), u.getUsername(), u.getRealName(), u.getRoleKey(),
                u.getOrgId(), u.getOrgName(), u.getDataScope(), homePath(u.getRoleKey())));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        // 无状态 JWT：删除本地 Token 即登出；生产可加 Redis 黑名单
        return Result.ok();
    }
}