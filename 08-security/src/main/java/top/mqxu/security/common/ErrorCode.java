package top.mqxu.security.common;

/**
 * 业务错误码。message 直接给前端展示，所以写中文。
 * <p>
 * 401 与 403 分开：401 是"没认出你是谁"，403 是"认出来了但不让进"。
 * 前端拿到 401 跳登录页，拿到 403 提示无权限，两者的处理路径不同。
 */
public enum ErrorCode {

    // 登录相关的 code 与 HTTP 状态码一致：密码错是 401，账号不可用是 403。
    // 前端按 code 就能决定"跳登录页"还是"提示联系管理员"，不必再判断 HTTP 状态。
    BAD_CREDENTIALS(401, "用户名或密码错误"),
    ACCOUNT_DISABLED(403, "账号已被禁用"),
    ACCOUNT_LOCKED(403, "账号已被锁定"),
    ACCOUNT_EXPIRED(403, "账号已过期"),
    CREDENTIALS_EXPIRED(403, "登录凭证已过期"),

    SMS_CODE_ERROR(30010, "验证码错误或已过期"),
    SMS_SEND_TOO_FREQUENT(30011, "验证码发送过于频繁，请稍后再试"),
    SMS_PHONE_NOT_REGISTERED(30012, "手机号未注册"),

    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有访问权限"),

    BAD_REQUEST(400, "请求参数不正确"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
