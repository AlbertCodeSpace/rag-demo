package com.albertstack.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class QdrantFilterTest {

    @Autowired
    VectorStore vectorStore;

    @BeforeEach
    void setUp() {
        // 清理上次运行残留的数据，保证测试可重复执行
        var filter = new FilterExpressionBuilder();
        vectorStore.delete(filter.in("source",
                "spring-ai-intro.txt", "rag-concepts.txt", "qdrant-guide.txt").build());

        vectorStore.add(List.of(
                new Document(
                        "Spring AI 提供了统一的 API 来对接各种大模型。",
                        Map.of("source", "spring-ai-intro.txt", "type", "tutorial", "chapter", 1)),
                new Document(
                        "RAG 通过检索外部知识来增强大模型的回答质量。",
                        Map.of("source", "rag-concepts.txt", "type", "tutorial", "chapter", 3)),
                new Document(
                        "Qdrant 的 HNSW 索引适用于高维向量的快速检索。",
                        Map.of("source", "qdrant-guide.txt", "type", "reference", "chapter", 8))
        ));
    }

    @Test
    void shouldFilterByEquality() {
        // 按 type 精确匹配
        var filter = new FilterExpressionBuilder();
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("Spring AI")
                        .topK(5)
                        .filterExpression(filter.eq("type", "tutorial").build())
                        .build());

        log.info("type=tutorial 结果数: {}", results.size());
        results.forEach(doc -> log.info("- [{}] {}", doc.getMetadata().get("source"), doc.getText()));

        // 过滤条件生效，只返回 tutorial 类型
        results.forEach(doc ->
                assertThat(doc.getMetadata().get("type")).isEqualTo("tutorial"));
    }

    @Test
    void shouldFilterByAndCondition() {
        // AND 组合：同时满足 type 和 source
        var filter = new FilterExpressionBuilder();
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("Spring AI")
                        .topK(5)
                        .filterExpression(filter.and(
                                filter.eq("type", "tutorial"),
                                filter.eq("source", "spring-ai-intro.txt")
                        ).build())
                        .build());

        log.info("AND 过滤结果数: {}", results.size());
        results.forEach(doc -> {
            assertThat(doc.getMetadata().get("type")).isEqualTo("tutorial");
            assertThat(doc.getMetadata().get("source")).isEqualTo("spring-ai-intro.txt");
        });
    }

    @Test
    void shouldFilterByInExpression() {
        // IN 查询：source 在指定列表中
        var filter = new FilterExpressionBuilder();
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("Spring AI")
                        .topK(5)
                        .filterExpression(filter.in("source",
                                "spring-ai-intro.txt", "rag-concepts.txt").build())
                        .build());

        log.info("IN 过滤结果数: {}", results.size());
        results.forEach(doc -> {
            String source = (String) doc.getMetadata().get("source");
            log.info("- [{}] {}", source, doc.getText());
            assertThat(source).isIn("spring-ai-intro.txt", "rag-concepts.txt");
        });
    }

    @Test
    void shouldFilterByComparison() {
        // 比较运算：chapter >= 3
        var filter = new FilterExpressionBuilder();
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("向量搜索")
                        .topK(5)
                        .filterExpression(filter.gte("chapter", 3).build())
                        .build());

        log.info("chapter >= 3 结果数: {}", results.size());
        results.forEach(doc -> {
            // Qdrant 内部把整数 payload 存为 Long，用 Number 接收避免类型不匹配
            int chapter = ((Number) doc.getMetadata().get("chapter")).intValue();
            log.info("- chapter={}, {}", chapter, doc.getText());
            assertThat(chapter).isGreaterThanOrEqualTo(3);
        });
    }
}