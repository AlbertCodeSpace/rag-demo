package com.albertstack.rag.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.albertstack.rag.service.RagService;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class ModularRagTest {

    @Autowired
    private RagService ragService;

    @Test
    void shouldAnswerPreciseQuery() {
        String question = "Spring AI 支持哪些向量数据库？";
        String answer = ragService.chat(question);
        log.info("精确查询 - 问题：{}", question);
        log.info("精确查询 - 回答：{}", answer);

        assertThat(answer).isNotBlank();
    }

    @Test
    void shouldHandleColloquialQuery() {
        // 口语化查询，关键词在但表述不规范，经过重写后应能正确检索
        String question = "spring ai的rag咋搞的";
        String answer = ragService.chat(question);
        log.info("口语化查询 - 问题：{}", question);
        log.info("口语化查询 - 回答：{}", answer);

        assertThat(answer).isNotBlank();
    }

    @Test
    void shouldStreamResponse() {
        String question = "什么是 RAG？";
        log.info("流式查询 - 问题：{}", question);

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