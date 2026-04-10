package com.albertstack.rag.service;

import com.albertstack.rag.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
public class RagService {

    private final ChatClient chatClient;
    // RewriteQueryTransformer 内部要再起一个 ChatClient，所以这里持有 Builder 而不是已建好的 ChatClient
    private final ChatClient.Builder chatClientBuilder;
    private final VectorStore vectorStore;
    // 旧 chat() 等方法仍然依赖这个不带 namespace filter 的全局 advisor
    private final RetrievalAugmentationAdvisor ragAdvisor;

    public RagService(ChatClient chatClient,
                      ChatClient.Builder chatClientBuilder,
                      VectorStore vectorStore,
                      RetrievalAugmentationAdvisor ragAdvisor) {
        this.chatClient = chatClient;
        this.chatClientBuilder = chatClientBuilder;
        this.vectorStore = vectorStore;
        this.ragAdvisor = ragAdvisor;
    }

    /**
     * 多知识库问答：限定 namespace 检索，返回答案 + 来源引用
     */
    public ChatResponse ask(String namespace, String question) {
        log.info("[{}] 收到问题: {}", namespace, question);

        var response = chatClient.prompt()
                .user(question)
                // 每次请求现场构建带 namespace filter 的 Advisor，原因见 buildNamespaceAdvisor 上的注释
                .advisors(buildNamespaceAdvisor(namespace))
                .call()
                .chatResponse();

        String answer = response.getResult().getOutput().getText();
        List<String> sources = extractSources(response);

        log.info("[{}] 回答完成，引用 {} 个来源", namespace, sources.size());
        return new ChatResponse(answer, sources);
    }

    /**
     * 多知识库流式问答：SSE 逐 token 推送
     */
    public Flux<String> askStream(String namespace, String question) {
        log.info("[{}] 收到流式问题: {}", namespace, question);
        return chatClient.prompt()
                .user(question)
                .advisors(buildNamespaceAdvisor(namespace))
                .stream()
                .content();
    }

    /**
     * 按 namespace 现场构建 RAG Advisor。
     * 之所以不像之前那样注册成单例 Bean，是因为 VectorStoreDocumentRetriever.filterExpression(...)
     * 接收的是一个静态 filter，构建后就固定了。要让 filter 跟着 namespace 变化，最简单的做法就是每次现场构建一个。
     * 重新构建只是几个对象分配，相比后面的 LLM 调用可以忽略。
     */
    private RetrievalAugmentationAdvisor buildNamespaceAdvisor(String namespace) {
        // 这一行是和之前管道相比唯一的新增：用 namespace 等值条件限定检索范围
        var filter = new FilterExpressionBuilder().eq("namespace", namespace).build();

        // 下面的管道结构和之前的 ragAdvisor 一致：查询重写 -> 带 filter 的检索 -> 上下文增强
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(RewriteQueryTransformer.builder()
                        .chatClientBuilder(chatClientBuilder).build())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(0.5)
                        .topK(5)
                        .filterExpression(filter)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
    }

    /**
     * 从 Advisor 上下文提取检索文档的 source，用于回答时标注引用。
     * 参数用全限定名，是因为本项目 DTO 包里也定义了 ChatResponse，避免和 Spring AI 的同名类冲突。
     */
    @SuppressWarnings("unchecked")
    private List<String> extractSources(org.springframework.ai.chat.model.ChatResponse response) {
        try {
            // RetrievalAugmentationAdvisor 执行完检索后，会把命中的 List<Document> 放进 response metadata 的 DOCUMENT_CONTEXT key 下
            Object context = response.getMetadata().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
            if (context instanceof List<?> docs) {
                return docs.stream()
                        .filter(Document.class::isInstance)
                        .map(Document.class::cast)
                        // 取每个文档的 source 元数据，去重后作为答案的引用列表
                        .map(doc -> doc.getMetadata().getOrDefault("source", "unknown").toString())
                        .distinct()
                        .toList();
            }
        } catch (Exception e) {
            // 来源提取只是辅助信息，挂了也不能阻断主问答流程，兜底返回空列表
            log.warn("提取来源失败: {}", e.getMessage());
        }
        return List.of();
    }

    // ========== 以下为前面章节遗留的旧方法，保持向后兼容 ==========

    public String chat(String question) {
        return chatClient.prompt()
                .user(question)
                .advisors(ragAdvisor)
                .call()
                .content();
    }

    public Flux<String> chatStream(String question) {
        return chatClient.prompt()
                .user(question)
                .advisors(ragAdvisor)
                .stream()
                .content();
    }

    public Flux<org.springframework.ai.chat.model.ChatResponse> chatStreamDetail(String question) {
        return chatClient.prompt()
                .user(question)
                .advisors(ragAdvisor)
                .stream()
                .chatResponse();
    }
}
