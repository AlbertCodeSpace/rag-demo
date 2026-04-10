# rag-demo

基于 **Spring Boot 4 + Spring AI 2.0.0-M3 + Ollama + Qdrant** 的多知识库 RAG（检索增强生成）示例项目。

提供按 namespace 隔离的知识库管理、多格式文档摄入、查询重写、向量检索、同步 / 流式问答与来源引用，覆盖一个最小但可用的 RAG 端到端链路。

> 配套教程：[Spring AI RAG 实战教程](https://albertstack.com/tutorials/spring-ai-rag)

## 功能特性

- **多知识库隔离**：以 URL path 上的 `namespace` 作为顶层资源段，所有摄入和检索都按 namespace 过滤，互不干扰。
- **多格式文档摄入**：内置 `pdf` / `md` / `txt` / `csv` 的专用 Reader，其余格式（docx、pptx、html 等）自动 fallback 到 Tika。
- **幂等更新**：上传同名文件时先按 `(namespace, source)` 删除旧分块，再写入新分块，避免残留。
- **Token 级分块**：使用 `TokenTextSplitter`（chunkSize=800，minChunkSizeChars=350）进行切分。
- **查询重写**：通过 `RewriteQueryTransformer` 在检索前对用户问题做重写，提升召回质量。
- **同步 + 流式问答**：同步接口返回完整答案 + 来源列表；流式接口通过 SSE 逐 token 推送，缓解首 token 等待。
- **来源引用**：从 `RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT` 提取命中文档的 `source` 元数据，作为答案的引用列表返回。
- **检索 / 答案评估**：`evaluation` 包下提供 `RetrievalEvaluator` 与 `AnswerEvaluator`，可对一组 `EvalCase` 跑离线评估。

## 技术栈

| 组件 | 版本 / 说明 |
| --- | --- |
| Java | 25 |
| Spring Boot | 4.0.5 |
| Spring AI | 2.0.0-M3（spring-ai-bom） |
| Web 层 | Spring WebFlux（响应式） |
| LLM / Embedding | Ollama（默认 chat: `qwen3.5:9b`，embedding: `qwen3-embedding:8b`） |
| 向量库 | Qdrant 1.14.0（gRPC 6334） |
| 文档读取 | spring-ai-pdf-document-reader / markdown-document-reader / tika-document-reader |
| 工具库 | Lombok |

## 项目结构

```
rag-demo/
├── docker-compose.yml              # Qdrant 本地服务
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/albertstack/rag/
    │   │   ├── RagDemoApplication.java
    │   │   ├── config/AiConfig.java               # ChatClient / RAG Advisor / 启动时摄入 demo 数据
    │   │   ├── controller/KnowledgeBaseController.java  # 上传 / 同步问答 / 流式问答
    │   │   ├── service/
    │   │   │   ├── IngestionService.java          # 多格式读取 + 分块 + 写入向量库
    │   │   │   └── RagService.java                # 按 namespace 现场构建 Advisor 并问答
    │   │   ├── dto/                               # ChatRequest / ChatResponse / IngestionResult
    │   │   ├── exception/                         # 业务异常 + 全局异常处理
    │   │   └── evaluation/                        # 检索 / 答案离线评估
    │   └── resources/
    │       ├── application.yaml
    │       └── docs/                              # 启动时自动摄入的 demo 文档
    └── test/java/com/albertstack/rag/...          # 各阶段的功能测试与对比测试
```

## 快速开始

### 1. 准备依赖

- JDK 25
- Maven（或使用项目自带的 `./mvnw`）
- Docker & Docker Compose
- 本地 Ollama，并预先拉取所需模型：

```bash
ollama pull qwen3.5:9b
ollama pull qwen3-embedding:8b
```

> `application.yaml` 中 `pull-model-strategy: when_missing`，理论上首次启动也会自动拉取，但模型较大，建议提前准备。

### 2. 启动 Qdrant

```bash
docker compose up -d
```

服务暴露：
- `6333` REST API + Web Dashboard（http://localhost:6333/dashboard）
- `6334` gRPC（Spring AI 使用）

### 3. 启动应用

```bash
./mvnw spring-boot:run
```

启动时会自动将 `src/main/resources/docs/` 下的几个示例文档摄入到 `namespace=demo`，方便立即测试。

## API 速览

所有端点都以 `/api/knowledge-bases/{namespace}` 为前缀。

### 上传文档

```bash
curl -X POST http://localhost:8080/api/knowledge-bases/demo/documents \
  -F "file=@/path/to/your.pdf"
```

返回：

```json
{ "namespace": "demo", "filename": "your.pdf", "chunks": 12 }
```

### 同步问答

```bash
curl -X POST http://localhost:8080/api/knowledge-bases/demo/chat \
  -H "Content-Type: application/json" \
  -d '{"question":"什么是 RAG？"}'
```

返回：

```json
{
  "answer": "...",
  "sources": ["rag-concepts.txt", "spring-ai-intro.txt"]
}
```

### 流式问答（SSE）

```bash
curl -N -X POST http://localhost:8080/api/knowledge-bases/demo/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"question":"什么是 RAG？"}'
```

逐 token 通过 `text/event-stream` 推送。

## 配置项

`src/main/resources/application.yaml`：

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: qwen3.5:9b
      embedding:
        model: qwen3-embedding:8b
    vectorstore:
      qdrant:
        host: localhost
        port: 6334
        collection-name: rag_demo
        initialize-schema: true
server:
  port: 8080
```

需要切换模型、Qdrant 地址或 collection 时直接改这里即可。

## 设计要点

- **按 namespace 现场构建 Advisor**：`VectorStoreDocumentRetriever.filterExpression(...)` 是构建期固定的，要让 filter 跟着 namespace 变化，最简单的做法是每次请求现场 build 一个 `RetrievalAugmentationAdvisor`，开销可忽略。详见 `RagService#buildNamespaceAdvisor`。
- **阻塞调用隔离**：摄入和 LLM 调用都是阻塞的，Controller 中通过 `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` 丢给弹性线程池，避免占住 Netty 事件循环。
- **临时文件兜底清理**：上传走 WebFlux 的 `FilePart`，先 `transferTo` 到临时文件再交给 DocumentReader，并在 `finally` 中 `deleteIfExists`，避免占满 `/tmp`。
- **来源提取容错**：`extractSources` 出现任何异常都只记录 warn 并返回空列表，不阻断主问答流程。

## 测试

```bash
./mvnw test
```

`src/test/java` 下覆盖了从底层组件（TextReader、TokenTextSplitter、Qdrant 读写、Embedding）到上层链路（ModularRag、QueryRewrite、MultiQueryExpand、RagCompare、KnowledgeBaseController）以及离线评估的多组测试。
