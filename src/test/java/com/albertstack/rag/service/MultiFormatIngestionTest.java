package com.albertstack.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class MultiFormatIngestionTest {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private VectorStore vectorStore;

    @Test
    void shouldIngestMultipleFormats() {
        // TXT -> TextReader
        int txtChunks = ingestionService.ingest(new ClassPathResource("docs/spring-ai-intro.txt"));
        log.info("TXT 摄入分块数：{}", txtChunks);

        // PDF -> PagePdfDocumentReader
        int pdfChunks = ingestionService.ingest(new ClassPathResource("docs/sample-report.pdf"));
        log.info("PDF 摄入分块数：{}", pdfChunks);

        // Markdown -> MarkdownDocumentReader
        int mdChunks = ingestionService.ingest(new ClassPathResource("docs/spring-boot-guide.md"));
        log.info("Markdown 摄入分块数：{}", mdChunks);

        // DOCX -> TikaDocumentReader（兜底）
        int docxChunks = ingestionService.ingest(new ClassPathResource("docs/meeting-notes.docx"));
        log.info("DOCX(Tika) 摄入分块数：{}", docxChunks);

        assertThat(txtChunks).isGreaterThan(0);
        assertThat(pdfChunks).isGreaterThan(0);
        assertThat(mdChunks).isGreaterThan(0);
        assertThat(docxChunks).isGreaterThan(0);

        // 跨格式检索
        var results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("Spring Boot 自动配置")
                        .topK(5)
                        .build()
        );
        log.info("跨格式检索结果数：{}", results.size());
        results.forEach(doc -> log.info("  来源：{}，内容预览：{}",
                doc.getMetadata().get("source"),
                doc.getText().substring(0, Math.min(80, doc.getText().length()))));

        assertThat(results).isNotEmpty();
    }

    @Test
    void shouldPreserveCustomMetadata() {
        var resource = new ClassPathResource("docs/spring-ai-intro.txt");
        ingestionService.ingest(resource, Map.of("category", "framework"));

        var results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("Spring AI")
                        .topK(1)
                        .build()
        );

        assertThat(results).isNotEmpty();
        log.info("自定义元数据：{}", results.get(0).getMetadata());
        assertThat(results.get(0).getMetadata())
                .containsEntry("category", "framework")
                .containsKey("source");
    }
}