package top.zff06.actuator.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 库存健康检查两个分支：UP 带判断依据，DOWN 带原因。
 * 字段是 @Value 注入的，测试里用 ReflectionTestUtils 直接设字段。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description InventoryHealthIndicatorTest
 **/
@DisplayName("库存健康检查")
class InventoryHealthIndicatorTest {

    private final InventoryHealthIndicator indicator = new InventoryHealthIndicator();

    @Test
    @DisplayName("依赖正常时返回 UP，并带上判断依据")
    void upWithDetails() {
        ReflectionTestUtils.setField(indicator, "healthy", true);
        ReflectionTestUtils.setField(indicator, "url", "http://inventory-service/health");

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsKeys("url", "latencyMs", "checkedAt")
                .containsEntry("url", "http://inventory-service/health");
    }

    @Test
    @DisplayName("依赖不通时返回 DOWN，并说明原因")
    void downWithReason() {
        ReflectionTestUtils.setField(indicator, "healthy", false);
        ReflectionTestUtils.setField(indicator, "url", "http://inventory-service/health");

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("reason", "连接超时");
    }
}
