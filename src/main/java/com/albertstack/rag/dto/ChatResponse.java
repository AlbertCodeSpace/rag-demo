package com.albertstack.rag.dto;

import java.util.List;

public record ChatResponse(
        String answer, // LLM 生成的回答正文
        List<String> sources // 本次回答引用的文档 source 列表，已去重
) {}
