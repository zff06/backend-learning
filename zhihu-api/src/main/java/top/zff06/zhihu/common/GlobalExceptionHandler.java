package top.zff06.zhihu.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * @auther zff06
 * @data 2026/10/20
 * @description 全局异常处理：任何异常都转成统一响应体，别把 500 堆栈直接甩给前端
 * <p>
 * 没有这层的话：参数传错、JSON 格式不对、代码空指针，前端拿到的都是 Spring 默认的
 * 错误页/堆栈 JSON，格式和正常响应完全不同，前端统一的 res.data.code 判断直接失效。
 **/
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 路径不存在（比如访问了没有映射的 URL） */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseResult handleNotFound(NoResourceFoundException e) {
        log.warn("接口不存在：{}", e.getResourcePath());
        return ResponseResult.builder().code(404).msg("接口不存在").build();
    }

    /** 参数类型不对，比如 ?size=abc 但接口要 int */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseResult handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String msg = "参数 " + e.getName() + " 格式不正确";
        log.warn("{}，实际值：{}", msg, e.getValue());
        return ResponseResult.builder().code(400).msg(msg).build();
    }

    /** 请求体不是合法 JSON，或字段类型对不上 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseResult handleUnreadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败：{}", e.getMessage());
        return ResponseResult.builder().code(400).msg("请求体格式不正确").build();
    }

    /** 兜底：剩下的都算服务端错误，日志里留完整堆栈，返回给前端的只有一句话 */
    @ExceptionHandler(Exception.class)
    public ResponseResult handleOther(Exception e) {
        log.error("未处理的异常", e);
        return ResponseResult.builder().code(500).msg("服务器内部错误").build();
    }
}
