package com.albertstack.rag.evaluation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
class RetrievalEvaluatorTest {

    @Autowired
    RetrievalEvaluator evaluator;

    @Test
    void shouldEvaluateRetrievalQuality() {
        var cases = List.of(
                new EvalCase("Spring AI 支持哪些向量数据库？", "Qdrant"),
                new EvalCase("Ollama 怎么管理模型？", "ollama"),
                new EvalCase("RAG 的核心思想是什么？", "检索"),
                new EvalCase("ChatClient 怎么使用？", "ChatClient"),
                new EvalCase("Embedding 是什么？", "向量")
                // 实际项目里建议准备 20-50 个用例以获得统计意义
        );

        var result = evaluator.evaluate(cases, 5);
        log.info("命中率={}, MRR={}", result.hitRate(), result.mrr());
    }
}