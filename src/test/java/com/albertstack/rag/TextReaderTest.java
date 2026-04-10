package com.albertstack.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class TextReaderTest {

    @Test
    void readTextFile() {
        var resource = new ClassPathResource("docs/spring-ai-intro.txt");
        var reader = new TextReader(resource);

        // read() 将整个文件读为一个 Document，纯文本没有天然分割边界
        List<Document> documents = reader.read();

        assertThat(documents).hasSize(1);
        log.info("内容长度：{}", documents.get(0).getText().length());

        // TextReader 自动添加 charset 和 source 元数据
        var metadata = documents.get(0).getMetadata();
        log.info("元数据：{}", metadata);
        assertThat(metadata).containsKeys("charset", "source");
    }
}