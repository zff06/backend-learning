package top.zff06.actuator.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务指标：四种仪表（Counter / 带标签 Counter / Gauge / Timer）的集中打点入口。
 *
 * <p>注意点：Gauge 必须绑定对象引用（AtomicInteger），抓取时回调读当前值；
 * 带标签的 Counter 与 Timer 按 name + tags 定位序列，每次调用现查是正常写法。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description BusinessMetrics
 **/
@Component
public class BusinessMetrics {

    private final MeterRegistry registry;

    /** 下单成功次数 */
    private final Counter ordersCreated;

    /** 当前处理中的订单数，Gauge 每次抓取时读它的当前值 */
    private final AtomicInteger processing = new AtomicInteger();

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.ordersCreated = Counter.builder("business.orders.created")
                .description("累计创建成功的订单数")
                .register(registry);
        registry.gauge("business.orders.processing", processing);
    }

    /**
     * 下单成功打点，计数加一。
     */
    public void orderCreated() {
        ordersCreated.increment();
    }

    /**
     * 下单失败打点，按原因分成独立序列。
     *
     * @param reason 失败原因（枚举值，不要用无限集合）
     */
    public void orderFailed(String reason) {
        registry.counter("business.orders.failed", "reason", reason).increment();
    }

    /**
     * 记录一次订单处理耗时。
     *
     * @param millis 耗时毫秒
     * @param result success / failure
     */
    public void recordHandle(long millis, String result) {
        Timer.builder("business.order.handle")
                .description("订单处理耗时")
                .tag("result", result)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record(Duration.ofMillis(millis));
    }

    /**
     * 进入处理，处理中订单数加一。
     */
    public void enter() {
        processing.incrementAndGet();
    }

    /**
     * 离开处理，处理中订单数减一。必须放在 finally 里调用。
     */
    public void leave() {
        processing.decrementAndGet();
    }
}
