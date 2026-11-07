package top.zff06.test.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理：六种失败路径对应六个处理器。
 *
 * <p>核心设计是 {@code e.getCode() % 1000}：业务码 30409 的后三位 409 就是 HTTP 状态。
 * HTTP 状态给监控和网关用，业务码给前端判断具体原因。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description GlobalExceptionHandler
 **/
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        // 业务码 30409 的后三位就是 HTTP 状态：30409 % 1000 = 409
        int http = e.getCode() % 1000;
        return ResponseEntity.status(http).body(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("参数校验失败");
        return ResponseEntity.badRequest().body(Result.fail(400, msg));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Result.fail(400, "请求体格式错误"));
    }

    /**
     * 教程 6.4 补的处理器之一：@RequestParam 类型转换失败。
     * 不接的话会落到兜底的 Exception 分支，客户端传错参数却收到 500。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型错误：{} = {}", e.getName(), e.getValue());
        return ResponseEntity.badRequest().body(Result.fail(400, "参数 " + e.getName() + " 格式不正确"));
    }

    /**
     * 教程 6.4 补的处理器之二：缺必填查询参数。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest().body(Result.fail(400, "缺少参数 " + e.getParameterName()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(500).body(Result.fail(500, "系统繁忙，请稍后再试"));
    }
}
