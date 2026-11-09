package top.mqxu.security.common;

/**
 * 统一响应体。过滤器链里的 401 / 403 是手写 JSON，格式与这里保持一致：
 * {"code":401,"message":"未登录或登录已过期","data":null}
 *
 * @param code    业务码，正常是 200
 * @param message 提示信息
 * @param data    业务数据
 */
public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "ok", data);
    }

    public static Result<Void> ok() {
        return new Result<>(200, "ok", null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
