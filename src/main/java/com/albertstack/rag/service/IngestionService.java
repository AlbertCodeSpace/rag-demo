package com.albertstack.rag.service;

import com.albertstack.rag.exception.DocumentProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class IngestionService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.splitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(350)
                .build();
    }

    /**
     * 摄入文档到指定 namespace。同名文件会先被删除再写入，实现幂等更新
     */
    public int ingest(Resource resource, String filename, String namespace) {
        // 先删除同 namespace 下的同名文件，避免重复上传产生残留分块
        deleteBySource(namespace, filename);

        List<Document> documents;
        try {
            documents = readDocuments(resource, filename);
        } catch (Exception e) {
            throw new DocumentProcessingException("文档读取失败: " + filename, e);
        }

        // 每个分块都打上 namespace 和 source 标签，供检索时过滤
        for (Document doc : documents) {
            doc.getMetadata().put("namespace", namespace);
            doc.getMetadata().put("source", filename);
        }

        List<Document> chunks = splitter.split(documents);
        vectorStore.add(chunks);

        log.info("[{}] 摄入完成: {} -> {} 个分块", namespace, filename, chunks.size());
        return chunks.size();
    }

    /**
     * 删除指定 namespace 下某个 source 的全部分块
     */
    public void deleteBySource(String namespace, String source) {
        var b = new FilterExpressionBuilder();
        var filter = b.and(b.eq("namespace", namespace), b.eq("source", source)).build();
        vectorStore.delete(filter);
    }

    // ========== 以下为前面章节遗留的旧重载，不带 namespace，保持向后兼容 ==========

    public int ingest(Resource resource, Map<String, Object> metadata) {
        List<Document> documents = readDocuments(resource, resource.getFilename());

        for (Document doc : documents) {
            doc.getMetadata().put("source", resource.getFilename());
            doc.getMetadata().putAll(metadata);
        }

        List<Document> chunks = splitter.split(documents);
        vectorStore.add(chunks);

        log.info("摄入完成：{} -> {} 个分块", resource.getFilename(), chunks.size());
        return chunks.size();
    }

    public int ingest(Resource resource) {
        return ingest(resource, Map.of());
    }

    private List<Document> readDocuments(Resource resource, String filename) {
        String ext = getFileExtension(filename);

        return switch (ext) {
            case "pdf" -> new PagePdfDocumentReader(resource).read();
            case "md" -> {
                var config = MarkdownDocumentReaderConfig.builder()
                        .withIncludeCodeBlock(true)
                        .withIncludeBlockquote(true)
                        .build();
                yield new MarkdownDocumentReader(resource, config).read();
            }
            case "txt", "csv" -> new TextReader(resource).read();
            default -> new TikaDocumentReader(resource).read();
        };
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}