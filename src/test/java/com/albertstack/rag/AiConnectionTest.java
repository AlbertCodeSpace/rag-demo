package com.albertstack.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class AiConnectionTest {

    @Autowired
    ChatClient chatClient;

    @Autowired
    EmbeddingModel embeddingModel;

    @Test
    void chatClientShouldRespond() {
        // 发送简单问题，验证模型能正常回复
        String response = chatClient.prompt()
                .user("用一句话介绍 Spring Boot")
                .call()
                .content();

        log.info("模型回复: {}", response);
        assertThat(response).isNotBlank();
    }

    @Test
    void embeddingModelShouldWork() {
        // 嵌入一段文本，验证 EmbeddingModel 正常工作
        float[] vector = embeddingModel.embed("Spring AI 是 Spring 官方的 AI 集成框架");

        log.info("向量维度: {}", vector.length);
        log.info("前 5 个分量: ");
        for (int i = 0; i < Math.min(5, vector.length); i++) {
            log.info("  [{}] = {}", i, String.format("%.6f", vector[i]));
        }

        // qwen3-embedding:8b 输出 2048 维向量
        assertThat(vector.length).isGreaterThan(0);
        assertThat(vector).isNotEmpty();
    }

}