package com.albertstack.rag.evaluation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class AnswerEvaluatorTest {

    @Autowired
    AnswerEvaluator answerEvaluator;

    @Test
    void shouldScoreAnswerQuality() {
        String question = "Spring AI 支持哪些向量数据库？";
        String expectedPoints = "Qdrant、Pinecone、Milvus、PGVector 等主流向量数据库，通过统一的 VectorStore 抽象屏蔽差异";

        // 用一对反差明显的答案做对照：好答案应该接近 5 分，差答案应该接近 1 分
        String goodAnswer = "Spring AI 通过 VectorStore 抽象支持 Qdrant、Pinecone、Milvus、PGVector 等主流向量数据库，切换实现只需改配置。";
        String badAnswer = "Spring AI 是一个前端可视化框架，主要用来画图表。";

        log.info("好答案: {}", goodAnswer);
        double goodScore = answerEvaluator.scoreAnswer(question, goodAnswer, expectedPoints);
        log.info("好答案得分: {}", goodScore);

        log.info("差答案: {}", badAnswer);
        double badScore = answerEvaluator.scoreAnswer(question, badAnswer, expectedPoints);
        log.info("差答案得分: {}", badScore);
    }
}