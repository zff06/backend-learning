package top.zff06.test.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单试算结果。
 *
 * <p>{@code appliedRules} 既给前端展示"已优惠 40 元"，也给测试提供额外的断言点：
 * 断言规则列表比只断言金额更能说明"哪条规则生效了"。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description OrderResult
 **/
public record OrderResult(BigDecimal originAmount,
                          BigDecimal discount,
                          BigDecimal finalAmount,
                          List<String> appliedRules) {
}
