package com.albertstack.rag.config;

import com.albertstack.rag.service.IngestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

@Slf4j
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public RetrievalAugmentationAdvisor ragAdvisor(
            ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(RewriteQueryTransformer.builder()
                        .chatClientBuilder(chatClientBuilder).build())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(0.5)
                        .topK(5)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
    }

    @Bean
    public CommandLineRunner ingestDocuments(IngestionService ingestionService) {
        return args -> {
            // 自动摄入的 demo 数据统一打上 namespace=demo 标签，让所有点都有 namespace 字段
            // 旧的 chat() 方法用的是不带 filter 的 ragAdvisor，依然能搜到这些数据
            String namespace = "demo";
            List<String> files = List.of(
                    "docs/spring-ai-intro.txt",
                    "docs/rag-concepts.txt",
                    "docs/ollama-guide.txt"
            );
            for (String file : files) {
                var resource = new ClassPathResource(file);
                String filename = resource.getFilename();
                int chunks = ingestionService.ingest(resource, filename, namespace);
                log.info("[{}] 已摄入: {} -> {} 个分块", namespace, file, chunks);
            }
        };
    }
}