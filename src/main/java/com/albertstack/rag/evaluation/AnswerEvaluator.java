package com.albertstack.rag.evaluation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnswerEvaluator {

    private final ChatClient chatClient;

    public double scoreAnswer(String question, String answer, String expectedPoints) {
        // 在 prompt 里明确给出 5 档评分标准，避免 LLM 用自己的标准随意打分
        String prompt = """
                你是一个严格的评估专家。请评估以下 RAG 系统的回答质量。

                评分标准（1-5 分）：
                - 5 分：完全正确，覆盖所有要点，没有幻觉
                - 4 分：基本正确，覆盖主要要点
                - 3 分：部分正确，遗漏了一些要点
                - 2 分：大部分不正确或不相关
                - 1 分：完全错误或答非所问

                问题：%s
                期望要点：%s
                实际回答：%s

                只返回一个数字（1-5），不要任何解释。
                """.formatted(question, expectedPoints, answer);

        String score = chatClient.prompt().user(prompt).call().content();

        try {
            return Double.parseDouble(score.trim());
        } catch (NumberFormatException e) {
            // LLM 偶尔不听话返回长篇解释，给个中性分而不是抛异常打断流程
            log.warn("LLM 返回了非数字评分: {}，默认给 3 分", score);
            return 3.0;
        }
    }
}