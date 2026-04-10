package com.albertstack.rag.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class QueryCompressionTest {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Test
    void shouldResolvePronouns() {
        var compressor = CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();

        // 模拟多轮对话历史
        List<Message> history = List.of(
                new UserMessage("Spring AI 支持哪些向量数据库？"),
                new AssistantMessage("Spring AI 支持 PGVector、Chroma、Milvus、Pinecone 等多种向量数据库。")
        );

        // 用户的跟进问题，包含代词"第一个"
        Query followUp = Query.builder()
                .text("第一个怎么配置？")
                .history(history)
                .build();

        Query compressed = compressor.transform(followUp);

        log.info("原始查询：{}", followUp.text());
        log.info("对话历史：{} 条消息", history.size());
        log.info("压缩后：{}", compressed.text());

        // 压缩后应该包含 PGVector，因为"第一个"指的是它
        assertThat(compressed.text()).containsIgnoringCase("PGVector");
    }
}