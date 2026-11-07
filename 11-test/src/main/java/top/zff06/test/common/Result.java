package top.zff06.test.common;

import lombok.Data;

/**
 * 统一响应结构。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description Result
 **/
@Data
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
