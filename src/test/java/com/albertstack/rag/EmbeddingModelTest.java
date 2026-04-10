package com.albertstack.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class EmbeddingModelTest {

    @Autowired
    EmbeddingModel embeddingModel;

    @Test
    void allEmbeddingApis() {
        // ========== 1. dimensions() — 获取模型向量维度 ==========
        int dims = embeddingModel.dimensions();
        log.info("模型向量维度: {}", dims);
        assertThat(dims).isEqualTo(4096); // qwen3-embedding:8b 输出 4096 维

        // ========== 2. embed(String) — 嵌入单个文本 ==========
        float[] vector = embeddingModel.embed("Spring AI 是什么？");
        log.info("embed(String) 返回维度: {}", vector.length);
        assertThat(vector).hasSize(dims);

        // ========== 3. embed(Document) — 嵌入 Document 对象 ==========
        var doc = new Document("RAG 通过检索外部文档增强 LLM 的回答",
                Map.of("source", "tutorial"));
        float[] docVector = embeddingModel.embed(doc);
        log.info("embed(Document) 返回维度: {}", docVector.length);
        assertThat(docVector).hasSize(dims);

        // ========== 4. call(EmbeddingRequest) — 批量嵌入 ==========
        EmbeddingResponse response = embeddingModel.call(
                new EmbeddingRequest(
                        List.of(
                                "Spring AI 是 Spring 官方的 AI 集成框架",
                                "RAG 增强了大语言模型的回答能力",
                                "向量数据库用于存储和检索高维向量"
                        ),
                        null // 使用默认选项
                )
        );
        var embeddings = response.getResults();
        log.info("call() 批量嵌入数量: {}", embeddings.size());
        assertThat(embeddings).hasSize(3);
        embeddings.forEach(e -> assertThat(e.getOutput()).hasSize(dims));

        // ========== 5. 语义相似度验证 ==========
        // 语义相近的两句话
        float[] v1 = embeddingModel.embed("Java 是一种面向对象的编程语言");
        float[] v2 = embeddingModel.embed("Java 是一门 OOP 编程语言");
        // 语义不同的一句话
        float[] v3 = embeddingModel.embed("今天的天气非常晴朗");

        double simSimilar = cosineSimilarity(v1, v2);
        double simDifferent = cosineSimilarity(v1, v3);

        log.info("相近文本相似度: {}", String.format("%.4f", simSimilar));
        log.info("不同文本相似度: {}", String.format("%.4f", simDifferent));

        // 语义相近的文本，相似度应该更高
        assertThat(simSimilar).isGreaterThan(simDifferent);
        assertThat(simSimilar).isGreaterThan(0.8);
        assertThat(simDifferent).isLessThan(0.6);
    }

    /**
     * 计算两个向量的余弦相似度
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}