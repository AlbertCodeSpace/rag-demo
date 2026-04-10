package com.albertstack.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class PdfIngestionTest {

    @Test
    void shouldReadPdfByPage() {
        var resource = new ClassPathResource("docs/sample-report.pdf");
        var reader = new PagePdfDocumentReader(resource);

        List<Document> pages = reader.read();
        log.info("PDF 总页数：{}", pages.size());

        assertThat(pages).isNotEmpty();

        // 验证每页都有 page_number 元数据
        for (Document page : pages) {
            assertThat(page.getMetadata()).containsKey("page_number");
            log.info("第 {} 页，内容长度：{} 字符",
                    page.getMetadata().get("page_number"),
                    page.getText().length());

            log.info("第 {} 页，前 100 字：{}",
                    page.getMetadata().get("page_number"),
                    page.getText().substring(0, Math.min(100, page.getText().length())));
        }

        // 二次分块
        var splitter = TokenTextSplitter.builder()
                .withChunkSize(200)
                .withMinChunkSizeChars(100)
                .build();
        List<Document> chunks = splitter.split(pages);
        log.info("分块后数量：{}", chunks.size());

        // 分块后仍保留页码元数据
        assertThat(chunks.get(0).getMetadata()).containsKey("page_number");
    }
}