package com.albertstack.rag.dto;

public record IngestionResult(
        String namespace, // 文件被写入的知识库标识
        String source, // 上传文件的原始文件名
        int chunkCount // 本次摄入生成的分块数量
) {}
