package com.albertstack.rag.dto;

public record ChatRequest(
        String question // 用户提问的原始文本，禁止为空或纯空白
) {
    // record 的紧凑构造器，反序列化阶段就会被调用，把不合法的请求挡在 Controller 之外
    public ChatRequest {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }
    }
}
