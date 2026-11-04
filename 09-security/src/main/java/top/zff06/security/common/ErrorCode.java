package top.zff06.security.common;

import lombok.Getter;

/**
 * 错误码枚举
 * 沿用 04-web 的业务规则：HTTP 状态码语义（200/400/401/403/404/500）
 * 加本项目自己的业务码（30001 起的业务异常）
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "参数校验失败"),
    UNAUTHORIZED(401, "未认证或令牌失效"),
    FORBIDDEN(403, "已认证但无权访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统繁忙，请稍后再试"),

    // 业务异常
    LOGIN_FAILED(30001, "用户名或密码错误"),
    ACCOUNT_DISABLED(30002, "账号已被禁用"),
    USER_NOT_FOUND(30003, "用户不存在"),
    USERNAME_EXISTS(30004, "用户名已存在"),
    CANNOT_DELETE_SELF(30005, "不能删除当前登录账号"),
    TASK_NOT_FOUND(30010, "任务不存在"),
    TASK_ALREADY_SUBMITTED(30011, "同一任务已提交，等待批改"),
    TASK_ALREADY_REVIEWED(30012, "该任务已批改，不能重复批改");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
