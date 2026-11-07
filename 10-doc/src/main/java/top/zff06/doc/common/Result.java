package top.zff06.doc.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统一响应体
 * <p>
 * Result 是泛型类，springdoc 在方法返回值上只知道 Result，猜不出 T；
 * 所以每个接口都要在 @ApiResponse 里显式声明 content = @Content(schema = @Schema(implementation = XxxView.class))。
 * 这里的 @Schema 只负责把三个字段的含义写进文档。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description Result
 **/
@Data
@Schema(description = "统一响应体")
public class Result<T> {

    @Schema(description = "业务状态码，200 表示成功", example = "200")
    private Integer code;

    @Schema(description = "提示信息", example = "success")
    private String message;

    @Schema(description = "业务数据，失败时为 null")
    private T data;

    protected Result() {
    }

    protected Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
