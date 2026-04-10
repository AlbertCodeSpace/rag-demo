package com.albertstack.rag.evaluation;

public record EvalResult(
        double hitRate, // 命中率：命中数 / 总用例数
        double mrr, // 平均倒数排名：Σ(1 / 命中位置) / 总用例数
        int totalCases,
        int hits
) {
    @Override
    public String toString() {
        return "EvalResult{hitRate=%.2f%%, mrr=%.4f, hits=%d/%d}"
                .formatted(hitRate * 100, mrr, hits, totalCases);
    }
}