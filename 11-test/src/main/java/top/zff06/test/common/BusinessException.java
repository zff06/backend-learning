package top.zff06.test.common;

import lombok.Getter;

/**
 * 业务异常：业务码 + 提示文案打包在一起。
 *
 * <p>两个静态工厂让测试可以直接断言 {@code BusinessException.stockNotEnough(5)}，
 * 不用记 30409 这个数字；但数字本身必须固定，它是前后端约定的一部分。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description BusinessException
 **/
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BusinessException stockNotEnough(int required) {
        return new BusinessException(30409, "库存不足，需要 " + required + " 件");
    }

    public static BusinessException amountInvalid() {
        return new BusinessException(30400, "金额必须大于 0");
    }
}
