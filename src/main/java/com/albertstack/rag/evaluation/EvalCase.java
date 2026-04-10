package com.albertstack.rag.evaluation;

public record EvalCase(
        String question, // 评估用例的提问
        String expectedContent // 期望检索结果中包含的文本片段
) {}