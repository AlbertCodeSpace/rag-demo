package com.albertstack.rag.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class QueryRewriteTest {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Test
    void shouldRewriteColloquialQuery() {
        var rewriter = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();

        // 口语化查询：关键词都在，但表述不够检索友好
        Query original = new Query("spring ai的rag咋搞的");
        Query rewritten = rewriter.transform(original);

        log.info("原始查询：{}", original.text());
        log.info("重写后：{}", rewritten.text());

        // 重写后应该包含核心关键词，且表述更规范
        assertThat(rewritten.text()).isNotEqualTo(original.text());
        assertThat(rewritten.text()).satisfiesAnyOf(
                t -> assertThat(t).containsIgnoringCase("Spring AI"),
                t -> assertThat(t).containsIgnoringCase("RAG")
        );
    }

    @Test
    void shouldImproveFragmentedQuery() {
        var rewriter = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();

        // 碎片化查询：缩写多、语法不完整
        Query original = new Query("embedding模型选哪个比较好 ollama上的");
        Query rewritten = rewriter.transform(original);

        log.info("原始查询：{}", original.text());
        log.info("重写后：{}", rewritten.text());

        assertThat(rewritten.text()).isNotBlank();
        // 重写后应该比原始查询更结构化
        log.info("原始长度：{}，重写后长度：{}", original.text().length(), rewritten.text().length());
    }
}