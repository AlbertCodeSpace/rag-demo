package com.albertstack.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class TokenTextSplitterTest {

    @Test
    void splitDocument() {
        // 先用 TextReader 读取文档
        var reader = new TextReader(new ClassPathResource("docs/spring-ai-intro.txt"));
        List<Document> documents = reader.read();

        // 配置分块参数（故意设小，让示例文本也能分出多块）
        var splitter = TokenTextSplitter.builder()
                .withChunkSize(200)          // 每块目标 200 Token（非字符，中文一字约 1-2 Token）
                .withMinChunkSizeChars(100)  // 不足 100 字符的尾块会合并到上一块
                .withKeepSeparator(true)     // 保留换行等分割符，上下文更完整
                .build();

        List<Document> chunks = splitter.split(documents);

        log.info("原始文档数：{}", documents.size());
        log.info("分块后数量：{}", chunks.size());
        assertThat(chunks.size()).isGreaterThan(documents.size());

        for (int i = 0; i < chunks.size(); i++) {
            log.info("块 {}：{} 字符", i, chunks.get(i).getText().length());
        }
    }
}