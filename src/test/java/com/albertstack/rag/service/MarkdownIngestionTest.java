package com.albertstack.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class MarkdownIngestionTest {

    @Test
    void shouldSplitByHeading() {
        var resource = new ClassPathResource("docs/spring-boot-guide.md");
        var config = MarkdownDocumentReaderConfig.builder()
                .withIncludeCodeBlock(true)
                .withIncludeBlockquote(true)
                .build();

        var reader = new MarkdownDocumentReader(resource, config);
        List<Document> docs = reader.read();

        log.info("Markdown 分割后文档数：{}", docs.size());
        assertThat(docs.size()).isGreaterThan(1);

        // 每个 Document 对应一个标题章节
        for (Document doc : docs) {
            log.info("标题层级：{}，内容预览：{}",
                    doc.getMetadata().get("category"),
                    doc.getText().substring(0, Math.min(80, doc.getText().length())));
        }

        // 验证元数据包含标题层级信息
        assertThat(docs.get(0).getMetadata()).containsKey("category");
    }
}