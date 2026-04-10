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
class QdrantWriteAndSearchTest {

    @Autowired
    VectorStore vectorStore;

    @BeforeEach
    void setUp() {
        // 清理上次运行残留的数据，保证测试可重复执行
        var filter = new FilterExpressionBuilder();
        vectorStore.delete(filter.in("source",
                "qdrant-intro.txt", "hnsw-guide.txt", "spring-boot-basics.txt").build());

        vectorStore.add(List.of(
                new Document(
                        "Qdrant 是用 Rust 编写的高性能向量数据库，支持高维向量的近似最近邻搜索。",
                        Map.of("source", "qdrant-intro.txt", "type", "tutorial")),
                new Document(
                        "HNSW 索引是一种高效的近似最近邻搜索算法，查询速度接近常数时间。",
                        Map.of("source", "hnsw-guide.txt", "type", "reference")),
                new Document(
                        "Spring Boot 的自动配置机制可以根据依赖自动注册相应的 Bean。",
                        Map.of("source", "spring-boot-basics.txt", "type", "tutorial"))
        ));
    }

    @Test
    void shouldPersistAndSearchDocuments() {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("向量数据库搜索")
                        .topK(2)
                        .build());

        log.info("搜索结果数量: {}", results.size());
        results.forEach(doc -> log.info("- [{}] {}", doc.getMetadata().get("source"), doc.getText()));

        assertThat(results).isNotEmpty();
        assertThat(results).hasSizeLessThanOrEqualTo(2);
    }
}