package top.zff06.test.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 订单试算请求。
 *
 * <p>record 是 Java 16 起的语法，一个头部就把四个字段、getter、equals、hashCode 全带上，
 * 测试里 {@code new OrderRequest(1L, new BigDecimal("300.00"), "GOLD", 1)} 一行就能造请求。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description OrderRequest
 **/
public record OrderRequest(
        @NotNull(message = "用户 ID 不能为空") Long userId,
        @NotNull(message = "金额不能为空")
        @DecimalMin(value = "0.01", message = "金额必须大于 0") BigDecimal amount,
        String memberLevel,
        @Min(value = 1, message = "数量至少为 1") Integer quantity) {

    /**
     * 紧凑构造器：没有参数列表，在正式赋值之前执行，用来补默认值。
     * 这里给参数重新赋值，赋的就是最后存进字段的值。
     */
    public OrderRequest {
        if (memberLevel == null || memberLevel.isBlank()) {
            memberLevel = "NORMAL";
        }
        if (quantity == null) {
            quantity = 1;
        }
    }
}
