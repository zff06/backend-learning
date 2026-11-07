package top.zff06.doc.common;

import lombok.Getter;

/**
 * 业务异常
 *
 * @auther zff06
 * @data 2026/11/7
 * @description BusinessException
 **/
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
