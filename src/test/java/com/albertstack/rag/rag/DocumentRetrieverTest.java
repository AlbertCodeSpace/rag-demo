package com.albertstack.rag.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;

@Slf4j
@SpringBootTest
class DocumentRetrieverTest {

    @Autowired
    private VectorStore vectorStore;

    @BeforeEach
    void setUp() {
        // 准备主题差异较大的文档，方便观察阈值过滤效果
        vectorStore.add(List.of(
                new Document("Spring AI 支持 PGVector、Chroma、Milvus 等向量数据库",
                        Map.of("source", "vector-db.txt")),
                new Document("Spring Boot Actuator 提供健康检查和监控端点",
                        Map.of("source", "actuator.txt")),
                new Document("Java 21 引入了虚拟线程，大幅提升并发性能",
                        Map.of("source", "java21.txt"))
        ));
    }

    @Test
    void shouldFilterByThreshold() {
        Query query = new Query("Spring AI 的向量数据库支持");

        // 低阈值：宽松匹配，可能返回"沾边"的文档
        var looseRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.1)
                .topK(5)
                .build();
        List<Document> looseResults = looseRetriever.retrieve(query);

        log.info("原始查询：{}", query.text());

        log.info("低阈值(0.1) 检索结果数：{}", looseResults.size());
        looseResults.forEach(doc -> log.info("  来源：{}，内容：{}",
                doc.getMetadata().get("source"),
                doc.getText().substring(0, Math.min(50, doc.getText().length()))));

        // 高阈值：严格匹配，只返回高度相关的文档
        var strictRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.8)
                .topK(5)
                .build();
        List<Document> strictResults = strictRetriever.retrieve(query);

        log.info("高阈值(0.8) 检索结果数：{}", strictResults.size());
        strictResults.forEach(doc -> log.info("  来源：{}，内容：{}",
                doc.getMetadata().get("source"),
                doc.getText().substring(0, Math.min(50, doc.getText().length()))));

        // 低阈值返回的文档数 >= 高阈值
        assertThat(looseResults.size()).isGreaterThanOrEqualTo(strictResults.size());
    }
}