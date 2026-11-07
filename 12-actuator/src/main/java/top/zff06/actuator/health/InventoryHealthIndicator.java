package top.zff06.actuator.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 自定义健康检查：模拟下游库存服务探活。
 *
 * <p>Bean 名字（inventory）就是 /actuator/health 响应里的组件名。
 * Boot 4 里 Health / HealthIndicator 在 org.springframework.boot.health.contributor 包下，
 * Boot 3 的 org.springframework.boot.actuate.health 已不存在。
 *
 * <p>withDetail 带上的 url / latencyMs / reason 是给人排查用的：
 * 只报 UP/DOWN 能知道"出问题了"，带上依据才知道"哪一环"。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description InventoryHealthIndicator
 **/
@Slf4j
@Component("inventory")
public class InventoryHealthIndicator implements HealthIndicator {

    @Value("${demo.inventory.healthy:true}")
    private boolean healthy;

    @Value("${demo.inventory.url:http://inventory-service/health}")
    private String url;

    @Override
    public Health health() {
        long start = System.currentTimeMillis();
        if (!healthy) {
            return Health.down()
                    .withDetail("url", url)
                    .withDetail("reason", "连接超时")
                    .build();
        }
        long cost = System.currentTimeMillis() - start;
        return Health.up()
                .withDetail("url", url)
                .withDetail("latencyMs", cost)
                .withDetail("checkedAt", System.currentTimeMillis())
                .build();
    }
}
