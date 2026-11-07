package top.zff06.test.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import top.zff06.test.client.InventoryClient;
import top.zff06.test.common.BusinessException;
import top.zff06.test.dto.OrderRequest;
import top.zff06.test.dto.OrderResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单试算规则：16 个用例的单元测试。
 *
 * <p>没有 Spring、没有 HTTP，被测对象直接 new，依赖用 Mockito 造。
 * 毫秒级，用来把 OrderService 的每个分支穷举一遍。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description OrderServiceTest
 **/
@DisplayName("订单试算规则")
class OrderServiceTest {

    private InventoryClient inventoryClient;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // 每个用例都拿一个新的 mock，避免用例之间互相影响
        inventoryClient = mock(InventoryClient.class);
        orderService = new OrderService(inventoryClient);
        // 默认给足库存，个别用例再覆盖
        when(inventoryClient.availableStock(anyLong())).thenReturn(100);
    }

    @Nested
    @DisplayName("满减规则")
    class FullCutRules {

        @Test
        @DisplayName("不满 100 不优惠")
        void noDiscountBelow100() {
            OrderResult result = orderService.calculate(
                    new OrderRequest(1L, new BigDecimal("99.00"), "NORMAL", 1));

            assertThat(result.discount()).isEqualByComparingTo("0.00");
            assertThat(result.finalAmount()).isEqualByComparingTo("99.00");
            assertThat(result.appliedRules()).isEmpty();
        }

        @Test
        @DisplayName("满 100 减 10")
        void discountAt100() {
            OrderResult result = orderService.calculate(
                    new OrderRequest(1L, new BigDecimal("100.00"), "NORMAL", 1));

            assertThat(result.discount()).isEqualByComparingTo("10.00");
            assertThat(result.finalAmount()).isEqualByComparingTo("90.00");
            assertThat(result.appliedRules()).containsExactly("满 100 减 10");
        }

        @Test
        @DisplayName("满 300 减 40，与满 100 不叠加")
        void discountAt300() {
            OrderResult result = orderService.calculate(
                    new OrderRequest(1L, new BigDecimal("300.00"), "NORMAL", 1));

            assertThat(result.discount()).isEqualByComparingTo("40.00");
            assertThat(result.finalAmount()).isEqualByComparingTo("260.00");
            assertThat(result.appliedRules()).containsExactly("满 300 减 40");
        }

        /**
         * 练习 10.1：先让这条红，再去 OrderService 加规则，再让它绿。
         */
        @Test
        @DisplayName("满 500 减 80")
        void discountAt500() {
            OrderResult result = orderService.calculate(
                    new OrderRequest(1L, new BigDecimal("500.00"), "NORMAL", 1));

            assertThat(result.discount()).isEqualByComparingTo("80.00");
            assertThat(result.finalAmount()).isEqualByComparingTo("420.00");
            assertThat(result.appliedRules()).containsExactly("满 500 减 80");
        }
    }

    @Nested
    @DisplayName("会员规则")
    class MemberRules {

        @Test
        @DisplayName("金卡在满减之后再打 95 折")
        void goldMemberExtraDiscount() {
            // 300 - 40 = 260，260 * 5% = 13，合计优惠 53，应付 247
            OrderResult result = orderService.calculate(
                    new OrderRequest(1L, new BigDecimal("300.00"), "GOLD", 1));

            assertThat(result.discount()).isEqualByComparingTo("53.00");
            assertThat(result.finalAmount()).isEqualByComparingTo("247.00");
            assertThat(result.appliedRules()).contains("满 300 减 40", "金卡会员 95 折");
        }

        /**
         * 练习 10.2-3：299.99 + 金卡这个组合，教程 9.5 节说没测过。
         *
         * <p>299.99 落在"满 100 减 10"区间，减完 289.99，再打 95 折省 14.4995，
         * 合计优惠 24.4995 四舍五入 24.50，应付 275.49。
         * 它和 300 元金卡的 247 差 28.49——两个独立的 if 各自测过，不等于组合测过。
         */
        @Test
        @DisplayName("299.99 金卡：只命中满 100 减 10，再打 95 折")
        void goldMemberJustBelow300() {
            OrderResult result = orderService.calculate(
                    new OrderRequest(1L, new BigDecimal("299.99"), "GOLD", 1));

            assertThat(result.discount()).isEqualByComparingTo("24.50");
            assertThat(result.finalAmount()).isEqualByComparingTo("275.49");
            assertThat(result.appliedRules()).containsExactly("满 100 减 10", "金卡会员 95 折");
        }

        /**
         * 练习 10.2-4：原来的三个方法（GOLD / gold / NORMAL）合成一个参数化方法。
         *
         * <p>null 那条走 CsvSource 的 nullValues 参数：把字面量 "null" 当成 null 传进来。
         * 紧凑构造器会把 null 补成 "NORMAL"，所以结果是不命中金卡折扣。
         */
        @ParameterizedTest(name = "等级 {0} 是否命中金卡折扣：{1}")
        @CsvSource(nullValues = "null", value = {
                "GOLD, true",
                "gold, true",
                "Gold, true",
                "NORMAL, false",
                "null, false"
        })
        @DisplayName("会员等级大小写不敏感，空值按普通会员处理")
        void memberLevelMatrix(String level, boolean goldHit) {
            OrderResult result = orderService.calculate(
                    new OrderRequest(1L, new BigDecimal("200.00"), level, 1));

            if (goldHit) {
                assertThat(result.appliedRules()).contains("金卡会员 95 折");
            } else {
                assertThat(result.appliedRules()).doesNotContain("金卡会员 95 折");
            }
        }
    }

    @Nested
    @DisplayName("边界与异常")
    class EdgeCases {

        @ParameterizedTest(name = "金额 {0} 最终应付 {1}")
        @CsvSource({
                "0.02, 0.02",
                "99.99, 99.99",
                "100.00, 90.00",
                "299.99, 289.99",
                "300.00, 260.00",
                "499.99, 459.99",
                "500.00, 420.00",
                "1000.00, 920.00"
        })
        @DisplayName("多组金额的应付结果")
        void finalAmountMatrix(String amount, String expected) {
            OrderResult result = orderService.calculate(
                    new OrderRequest(1L, new BigDecimal(amount), "NORMAL", 1));

            assertThat(result.finalAmount()).isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("金额为 0 抛业务异常")
        void zeroAmount() {
            assertThatThrownBy(() -> orderService.calculate(
                    new OrderRequest(1L, BigDecimal.ZERO, "NORMAL", 1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("金额必须大于 0");
        }

        @Test
        @DisplayName("库存不足抛业务异常")
        void stockNotEnough() {
            when(inventoryClient.availableStock(anyLong())).thenReturn(2);

            assertThatThrownBy(() -> orderService.calculate(
                    new OrderRequest(1L, new BigDecimal("100.00"), "NORMAL", 5)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("库存不足");
        }

        @Test
        @DisplayName("库存够时不会抛异常，且只查一次库存")
        void stockQueriedOnce() {
            orderService.calculate(new OrderRequest(1L, new BigDecimal("100.00"), "NORMAL", 1));

            verify(inventoryClient, times(1)).availableStock(anyLong());
        }

        @Test
        @DisplayName("金额极小时不低于 0.01")
        void minimumAmount() {
            OrderResult result = orderService.calculate(
                    new OrderRequest(1L, new BigDecimal("0.01"), "NORMAL", 1));

            assertThat(result.finalAmount()).isEqualByComparingTo("0.01");
        }

        /**
         * 练习 10.2-2：补上 OrderService 里 {@code amount == null} 那条从没走过的分支。
         *
         * <p>它属于三层校验的第三层（Service 内的判断）。证明的是：绕过 Web 层直接调 Service 时，
         * 空金额也会被拦住，抛出的是业务异常而不是 NPE。
         * 没证明的是：Web 层会不会传 null 进来——那由 @NotNull 负责，这里不关心。
         */
        @Test
        @DisplayName("金额为 null 抛业务异常")
        void nullAmount() {
            assertThatThrownBy(() -> orderService.calculate(
                    new OrderRequest(1L, null, "NORMAL", 1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("金额必须大于 0");
        }
    }
}
