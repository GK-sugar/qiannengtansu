package com.qnct.dto;

public record UserInfoDto(
        Long id,
        String username,
        String realName,
        String roleKey,
        Long orgId,
        String orgName,
        String dataScope,
        String homePath) {
}