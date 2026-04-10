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
class MetadataFlowTest {

    @Test
    void metadataSurvivesThroughPipeline() {
        // 1. Reader 自动添加 source、charset
        var reader = new TextReader(new ClassPathResource("docs/spring-ai-intro.txt"));
        List<Document> docs = reader.read();
        log.info("Reader 元数据：{}", docs.get(0).getMetadata());
        // {charset=UTF-8, source=spring-ai-intro.txt}

        // 2. 手动添加业务元数据
        reader.getCustomMetadata().put("category", "framework");
        reader.getCustomMetadata().put("author", "Albert");
        docs = reader.read();
        log.info("自定义元数据：{}", docs.get(0).getMetadata());
        // {charset=UTF-8, source=spring-ai-intro.txt, category=framework, author=Albert}

        assertThat(docs.get(0).getMetadata()).containsKeys("source", "category", "author");

        // 3. Splitter 分块后，每个 chunk 继承父文档的全部元数据
        var splitter = TokenTextSplitter.builder().build();
        List<Document> chunks = splitter.split(docs);

        for (Document chunk : chunks) {
            log.info("块元数据：{}", chunk.getMetadata());
            assertThat(chunk.getMetadata()).containsKeys("charset", "source", "category", "author");
        }
    }
}