package com.albertstack.rag.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 业务异常：文档处理失败、参数无效等可预期错误
    @ExceptionHandler({DocumentProcessingException.class, IllegalArgumentException.class})
    public ProblemDetail handleBusiness(RuntimeException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("请求处理失败");
        return problem;
    }

    // 请求体反序列化失败：record compact constructor 抛出的校验异常会被包成这种类型
    @ExceptionHandler(ServerWebInputException.class)
    public ProblemDetail handleInputError(ServerWebInputException ex) {
        // 优先取最底层的原因信息，方便前端看到 "问题不能为空" 这类业务提示
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String detail = root.getMessage() != null ? root.getMessage() : "请求格式错误";
        log.warn("请求输入错误: {}", detail);
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("请求处理失败");
        return problem;
    }

    // 兜底：所有未处理的异常统一返回 500
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("服务内部错误", ex);
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "服务处理请求时发生错误");
        problem.setTitle("服务器错误");
        return problem;
    }
}
