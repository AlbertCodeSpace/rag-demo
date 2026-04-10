package com.albertstack.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class VectorStoreTest {

    @Autowired
    EmbeddingModel embeddingModel;

    @TempDir
    Path tempDir;

    SimpleVectorStore vectorStore;

    @BeforeEach
    void setUp() {
        // 创建向量存储，传入 EmbeddingModel 用于自动生成向量
        vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        // 准备测试文档，每个文档携带元数据用于后续过滤
        var docs = List.of(
                new Document(
                        "Spring AI 是 Spring 官方推出的 AI 集成框架，提供统一 API 对接各种 AI 模型",
                        Map.of("source", "spring-ai-docs", "type", "introduction")
                ),
                new Document(
                        "RAG 全称 Retrieval-Augmented Generation，通过检索外部知识增强大模型的回答质量",
                        Map.of("source", "rag-tutorial", "type", "concept")
                ),
                new Document(
                        "SimpleVectorStore 是 Spring AI 内置的内存向量存储，适合开发和测试使用",
                        Map.of("source", "spring-ai-docs", "type", "component")
                ),
                new Document(
                        "PGVector 是 PostgreSQL 的向量扩展，适合生产环境的大规模向量检索",
                        Map.of("source", "pgvector-docs", "type", "component")
                ),
                new Document(
                        "Ollama 可以在本地运行大语言模型，支持 Llama、Qwen 等开源模型",
                        Map.of("source", "ollama-docs", "type", "tool")
                )
        );

        // add() 内部自动调用 embeddingModel.embed() 生成向量，然后保存到内存
        vectorStore.add(docs);
    }

    /**
     * 基础相似度搜索：topK 控制返回数量
     */
    @Test
    void similaritySearch() {
        // topK=3 表示返回最相似的 3 个结果
        var results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("什么是 RAG？")
                        .topK(3)
                        .build()
        );

        // 内部流程：query 先被 embed 为向量，再与所有文档向量计算余弦相似度，按得分降序返回
        for (Document doc : results) {
            log.info("相似度: {} | 来源: {} | 内容: {}",
                    String.format("%.4f", doc.getScore()),
                    doc.getMetadata().get("source"),
                    doc.getText().substring(0, Math.min(50, doc.getText().length()))
            );
        }

        // 关于 RAG 的文档应该排在最前面
        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().getText()).contains("RAG");
    }

    /**
     * similarityThreshold：过滤低相关结果
     */
    @Test
    void searchWithThreshold() {
        // 设置 similarityThreshold=0.6，低于此相似度的结果直接丢弃
        var results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("什么是 RAG？")
                        .topK(5)
                        .similarityThreshold(0.6)
                        .build()
        );

        log.info("高相似度结果数: {}", results.size());

        // 所有返回结果的 score 都应 >= 0.6
        results.forEach(doc ->
                assertThat(doc.getScore()).isGreaterThanOrEqualTo(0.6)
        );
    }

    /**
     * filterExpression：基于元数据过滤，缩小检索范围
     */
    @Test
    void searchWithFilter() {
        // 只在 source="spring-ai-docs" 的文档中搜索
        var filtered = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("Spring AI")
                        .topK(3)
                        .filterExpression("source == 'spring-ai-docs'")
                        .build()
        );

        // 返回结果的 source 都是 spring-ai-docs
        filtered.forEach(doc ->
                assertThat(doc.getMetadata().get("source")).isEqualTo("spring-ai-docs")
        );
    }

    /**
     * 持久化：save() 保存到 JSON 文件，load() 恢复到新实例
     */
    @Test
    void saveAndLoad() {
        File saveFile = tempDir.resolve("vector-store.json").toFile();

        // 保存：将所有文档及其向量序列化为 JSON
        vectorStore.save(saveFile);
        assertThat(saveFile).exists();
        log.info("文件大小: {} bytes", saveFile.length());

        // 加载：创建新实例，从文件恢复数据
        var restoredStore = SimpleVectorStore.builder(embeddingModel).build();
        restoredStore.load(saveFile);

        // 验证加载后的搜索结果与原始一致
        var original = vectorStore.similaritySearch(
                SearchRequest.builder().query("什么是 RAG？").topK(3).build()
        );
        var restored = restoredStore.similaritySearch(
                SearchRequest.builder().query("什么是 RAG？").topK(3).build()
        );

        assertThat(restored).hasSameSizeAs(original);
        assertThat(restored.getFirst().getText()).isEqualTo(original.getFirst().getText());
    }
}