package top.zff06.doc.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 错误码枚举
 * <p>
 * 枚举本身也会进文档：字段上的 @Schema 决定 UI 里显示的是"裸值"还是"值 + 说明"。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description ErrorCode
 **/
@Getter
@Schema(description = "业务错误码")
public enum ErrorCode {

    SUCCESS(200, "success"),

    @Schema(description = "参数错误")
    BAD_REQUEST(400, "参数错误"),

    @Schema(description = "资源不存在")
    NOT_FOUND(404, "资源不存在"),

    @Schema(description = "系统繁忙，请稍后再试")
    INTERNAL_ERROR(500, "系统繁忙，请稍后再试"),

    BOOK_NOT_FOUND(30001, "图书不存在"),

    ISBN_DUPLICATED(30002, "ISBN 已存在");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
