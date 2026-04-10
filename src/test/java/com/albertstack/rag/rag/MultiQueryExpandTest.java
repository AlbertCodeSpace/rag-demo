package com.albertstack.rag.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class MultiQueryExpandTest {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Test
    void shouldExpandToMultipleQueries() {
        var expander = MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .numberOfQueries(3)
                .build();

        Query original = new Query("Spring AI 如何实现 RAG");
        List<Query> expanded = expander.expand(original);

        log.info("原始查询：{}", original.text());
        log.info("扩展结果（共 {} 个）：", expanded.size());
        for (int i = 0; i < expanded.size(); i++) {
            log.info("  查询 {}：{}", i + 1, expanded.get(i).text());
        }

        // includeOriginal 默认为 true，所以结果数 = 原始 1 个 + 变体 3 个 = 4 个
        assertThat(expanded).hasSize(4);
        // 第一个应该是原始查询
        assertThat(expanded.get(0).text()).isEqualTo(original.text());
    }

    @Test
    void shouldExcludeOriginalWhenConfigured() {
        var expander = MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .numberOfQueries(3)
                .includeOriginal(false) // 不保留原始查询
                .build();

        Query original = new Query("Spring AI 如何实现 RAG");
        List<Query> expanded = expander.expand(original);

        log.info("excludeOriginal 模式，扩展结果（共 {} 个）：", expanded.size());
        for (int i = 0; i < expanded.size(); i++) {
            log.info("  查询 {}：{}", i + 1, expanded.get(i).text());
        }

        // 只有 LLM 生成的变体，没有原始查询
        assertThat(expanded).hasSize(3);
    }
}