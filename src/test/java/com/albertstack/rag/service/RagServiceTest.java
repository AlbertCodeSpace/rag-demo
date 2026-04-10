package com.albertstack.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class RagServiceTest {

    // AiConfig 中的 CommandLineRunner 会在测试启动时自动摄入知识库

    @Autowired
    private RagService ragService;

    @Test
    void shouldAnswerFromKnowledgeBase() {
        String question = "Spring AI 支持哪些向量数据库？";
        String answer = ragService.chat(question);

        log.info("问题：{}", question);
        log.info("回答：{}", answer);

        assertThat(answer).satisfiesAnyOf(
                a -> assertThat(a).containsIgnoringCase("PGVector"),
                a -> assertThat(a).containsIgnoringCase("SimpleVectorStore"),
                a -> assertThat(a).containsIgnoringCase("Chroma")
        );
    }

    @Test
    void shouldAnswerAboutOllama() {
        String question = "Ollama 支持哪些模型？";
        String answer = ragService.chat(question);

        log.info("问题：{}", question);
        log.info("回答：{}", answer);

        assertThat(answer).satisfiesAnyOf(
                a -> assertThat(a).containsIgnoringCase("LLaMA"),
                a -> assertThat(a).containsIgnoringCase("Mistral"),
                a -> assertThat(a).containsIgnoringCase("Qwen")
        );
    }

    @Test
    void shouldStreamWithThinking() {
        String question = "RAG 的核心组件有哪些？";
        log.info("问题：{}", question);

        var thinkingText = new StringBuilder();
        var contentText = new StringBuilder();

        ragService.chatStreamDetail(question)
                .doOnNext(chunk -> {
                    var result = chunk.getResult();
                    if (result == null || result.getOutput() == null) return;

                    // 思考过程在 Generation metadata 的 "thinking" 字段中
                    Object thinking = result.getMetadata().get("thinking");
                    if (thinking != null && !thinking.toString().isEmpty()) {
                        thinkingText.append(thinking);
                        System.out.print(thinking);
                        return;
                    }

                    // 回答内容
                    String text = result.getOutput().getText();
                    if (text != null && !text.isEmpty()) {
                        contentText.append(text);
                        System.out.print(text);
                    }
                })
                .doOnComplete(System.out::println)
                .blockLast();

        log.info("思考过程：{} 字符", thinkingText.length());
        log.info("回答内容：{} 字符", contentText.length());

        assertThat(contentText.toString()).isNotBlank();
    }
}