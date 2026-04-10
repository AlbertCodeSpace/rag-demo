package com.albertstack.rag.controller;

import com.albertstack.rag.service.IngestionService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// timeout 设到 180 秒：RAG 包含查询重写、向量检索、LLM 生成三段，本地 Ollama 跑大模型耗时不少
@AutoConfigureWebTestClient(timeout = "180s")
// PER_CLASS 让 @BeforeAll 可以是非 static 方法，方便注入 Spring 管理的 IngestionService 做清理
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KnowledgeBaseControllerTest {

    private static final String NAMESPACE = "test-kb";

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    IngestionService ingestionService;

    @BeforeAll
    void setup() {
        // Qdrant 数据落盘，重复跑测试会累积。先按 (namespace, source) 清掉残留，保证测试可重复执行
        ingestionService.deleteBySource(NAMESPACE, "spring-ai-intro.txt");
    }

    @Test
    void shouldUploadFileToNamespace() {
        var resource = new ClassPathResource("docs/spring-ai-intro.txt");
        var bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", resource).filename("spring-ai-intro.txt");

        log.info("上传文件: namespace={}, file=spring-ai-intro.txt", NAMESPACE);
        byte[] body = webTestClient.post()
                .uri("/api/knowledge-bases/{ns}/documents", NAMESPACE)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(bodyBuilder.build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();
        log.info("上传响应: {}", new String(body));
    }

    @Test
    void shouldAnswerWithSources() {
        // 测试用例不依赖执行顺序，每个 case 自己保证前置数据。
        // 这里再上传一次同样的文件是幂等的：deleteBySource 会先把旧分块清掉再写入新的
        var resource = new ClassPathResource("docs/spring-ai-intro.txt");
        var bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", resource).filename("spring-ai-intro.txt");
        webTestClient.post()
                .uri("/api/knowledge-bases/{ns}/documents", NAMESPACE)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(bodyBuilder.build())
                .exchange()
                .expectStatus().isOk();

        String question = "Spring AI 是什么？";
        log.info("发起问答: namespace={}, question={}", NAMESPACE, question);
        byte[] body = webTestClient.post()
                .uri("/api/knowledge-bases/{ns}/chat", NAMESPACE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\": \"" + question + "\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();
        log.info("问答响应: {}", new String(body));
    }

    @Test
    void shouldRejectEmptyQuestion() {
        // 空问题会触发 ChatRequest 紧凑构造器抛 IllegalArgumentException，
        // 被 WebFlux 包成 ServerWebInputException，最后由 GlobalExceptionHandler 转成 400 + ProblemDetail
        log.info("发起空问题，预期被拦截");
        byte[] body = webTestClient.post()
                .uri("/api/knowledge-bases/{ns}/chat", NAMESPACE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\": \"\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .returnResult()
                .getResponseBody();
        log.info("拦截响应: {}", new String(body));
    }
}
