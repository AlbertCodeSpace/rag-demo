package com.albertstack.rag.controller;

import com.albertstack.rag.dto.ChatRequest;
import com.albertstack.rag.dto.ChatResponse;
import com.albertstack.rag.dto.IngestionResult;
import com.albertstack.rag.service.IngestionService;
import com.albertstack.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
// 路径以 namespace 为顶层资源段，所有端点天然按知识库分组
@RequestMapping("/api/knowledge-bases/{namespace}")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final IngestionService ingestionService;
    private final RagService ragService;

    /**
     * 上传文件到指定知识库。同名文件会自动覆盖（先删后写）
     */
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<IngestionResult> upload(@PathVariable String namespace,
                                        @RequestPart("file") FilePart filePart) {
        // 摄入是阻塞 IO，包到 fromCallable + boundedElastic 里，避免占住 Netty 事件循环线程
        return Mono.fromCallable(() -> {
            // FilePart 是 WebFlux 的响应式抽象，但 DocumentReader 都要 Resource，所以先落盘成临时文件
            Path tempFile = Files.createTempFile("rag-upload-", "-" + filePart.filename());
            filePart.transferTo(tempFile).block();
            try {
                int chunks = ingestionService.ingest(
                        new FileSystemResource(tempFile),
                        filePart.filename(),
                        namespace);
                return new IngestionResult(namespace, filePart.filename(), chunks);
            } finally {
                // try-finally 保证临时文件一定被清理，避免占满 /tmp
                Files.deleteIfExists(tempFile);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 同步问答：返回完整答案 + 来源引用，适合后台对接和需要完整答案的场景
     */
    @PostMapping("/chat")
    public Mono<ChatResponse> chat(@PathVariable String namespace,
                                   @RequestBody ChatRequest request) {
        // LLM 调用同样是阻塞的，按摄入端点的同样套路丢给弹性线程池
        return Mono.fromCallable(() -> ragService.ask(namespace, request.question()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 流式问答：适合面向终端用户的交互，缓解首 token 等待焦虑
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@PathVariable String namespace,
                                   @RequestBody ChatRequest request) {
        // 直接返回 Flux<String>，Spring WebFlux 会自动按 SSE 格式编码，每个 token 一个 data: 帧
        return ragService.askStream(namespace, request.question());
    }
}