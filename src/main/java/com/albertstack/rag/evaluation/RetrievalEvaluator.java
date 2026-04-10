package com.albertstack.rag.evaluation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetrievalEvaluator {

    private final VectorStore vectorStore;

    public EvalResult evaluate(List<EvalCase> cases, int topK) {
        int hits = 0;
        double mrrSum = 0;

        for (var evalCase : cases) {
            var results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(evalCase.question())
                            .topK(topK)
                            .build()
            );

            // 用 contains 而不是精确匹配，符合"找到相关段落即算命中"的直觉
            for (int i = 0; i < results.size(); i++) {
                if (results.get(i).getText().contains(evalCase.expectedContent())) {
                    hits++;
                    // i 从 0 起，加 1 后第一名得 1 分、第二名得 0.5 分
                    mrrSum += 1.0 / (i + 1);
                    break; // 一个用例只算排名最高的命中，避免重复计分
                }
            }
        }

        var result = new EvalResult(
                (double) hits / cases.size(),
                mrrSum / cases.size(),
                cases.size(),
                hits
        );
        log.info("评估结果: {}", result);
        return result;
    }
}