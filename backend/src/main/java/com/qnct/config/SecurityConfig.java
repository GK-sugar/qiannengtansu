package com.qnct.config;

import com.qnct.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置：JWT 无状态 + 按四端角色做 URL 级隔离。
 * 数据隔离（省/市/企业三级）在业务层靠实体 data_scope / org_id 拦截器加固，
 * 详见 RbacInterceptor（演示模式由各 Controller 按 LoginUser.orgId 过滤）。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // SSE/异步 dispatch 不再二次鉴权，否则流式响应中途 Access Denied 被截断
                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ASYNC).permitAll()
                .requestMatchers(
                        "/api/v1/auth/**",          // 登录/验证码/公开
                        "/api/v1/public/**",        // 扫码验真（免登录）
                        "/actuator/health",
                        "/error"
                ).permitAll()
                .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_SUPER_ADMIN")
                .requestMatchers("/api/v1/gov/**").hasAnyAuthority("ROLE_GOV", "ROLE_SUPER_ADMIN")
                .requestMatchers("/api/v1/agency/**").hasAnyAuthority("ROLE_AGENCY", "ROLE_SUPER_ADMIN")
                .requestMatchers("/api/v1/enterprise/**").hasAuthority("ROLE_ENTERPRISE")
                .requestMatchers("/api/v1/national/**").hasAuthority("ROLE_NATIONAL")
                .anyRequest().authenticated())
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"code\":401,\"msg\":\"未登录或Token已过期\",\"data\":null}");
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(403);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"code\":403,\"msg\":\"无权访问\",\"data\":null}");
                }))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 密码编码：正式对接 sys_user 时启用 BCrypt */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}