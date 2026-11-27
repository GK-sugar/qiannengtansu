package com.qnct.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ChatRequest(
        @NotBlank String message,
        List<ChatMessage> history,
        String context) {

    public record ChatMessage(String role, String content) {}
}