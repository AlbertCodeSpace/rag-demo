package com.albertstack.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class TikaIngestionTest {

    @Test
    void shouldReadDocxWithTika() {
        var resource = new ClassPathResource("docs/meeting-notes.docx");
        var reader = new TikaDocumentReader(resource);

        List<Document> docs = reader.read();
        log.info("Tika 提取文档数：{}", docs.size());
        log.info("提取文本长度：{} 字符", docs.get(0).getText().length());
        log.info("文本预览：{}", docs.get(0).getText().substring(0, Math.min(200, docs.get(0).getText().length())));

        // Tika 把整个文件提取为一个 Document
        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).getText()).isNotBlank();
    }
}