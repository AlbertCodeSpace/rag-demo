package com.albertstack.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class RagCompareTest {

    @Autowired
    private ChatClient chatClient;  // 基础版，无 Advisor

    @Autowired
    @Qualifier("ragChatClient")
    private ChatClient ragChatClient;  // 带 QuestionAnswerAdvisor

    @Test
    void compareWithAndWithoutRag() {
        String question = "Spring AI 支持哪些向量数据库？";

        // 无 RAG：模型凭训练数据回答，可能不准确
        String plainAnswer = chatClient.prompt()
                .user(question)
                .call()
                .content();
        log.info("无 RAG 回答：\n{}", plainAnswer);

        // 有 RAG：基于知识库文档回答
        String ragAnswer = ragChatClient.prompt()
                .user(question)
                .call()
                .content();
        log.info("有 RAG 回答：\n{}", ragAnswer);

        // RAG 版本应该包含知识库中的具体内容
        assertThat(ragAnswer).satisfiesAnyOf(
                a -> assertThat(a).containsIgnoringCase("SimpleVectorStore"),
                a -> assertThat(a).containsIgnoringCase("PGVector"),
                a -> assertThat(a).containsIgnoringCase("Chroma")
        );
    }

    @Test
    void ragShouldAnswerSpecificFacts() {
        // 知识库中明确写了 http://localhost:11434
        String answer = ragChatClient.prompt()
                .user("Ollama 默认运行在哪个端口？")
                .call()
                .content();
        log.info("回答：{}", answer);

        assertThat(answer).contains("11434");
    }
}