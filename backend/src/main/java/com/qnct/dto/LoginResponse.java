package com.qnct.dto;

public record LoginResponse(String token, UserInfoDto user) {
}