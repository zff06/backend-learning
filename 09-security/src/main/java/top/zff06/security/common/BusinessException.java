package top.zff06.security.common;

import lombok.Getter;

/**
 * 业务异常，携带错误码，由全局异常处理器统一转成响应体
 */
public class BusinessException extends RuntimeException {

    /** 错误码可能不是枚举里的固定值（如自定义提示），所以单独存一份 */
    @Getter
    private final Integer code;

    @Getter
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.code = errorCode.getCode();
    }

    /** 个别场景需要自定义提示文案时用 */
    public BusinessException(Integer code, String message) {
        super(message);
        this.errorCode = null;
        this.code = code;
    }
}
