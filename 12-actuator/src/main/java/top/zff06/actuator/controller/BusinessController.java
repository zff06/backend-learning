package top.zff06.actuator.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.actuator.common.Result;
import top.zff06.actuator.metrics.BusinessMetrics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 产生指标的业务接口。业务本身只是陪衬，存在的意义是让指标有数据。
 *
 * <p>enter() / leave() 包在 try / finally 里：方法抛异常时 leave 不执行的话，
 * Gauge 会只增不减，监控上是一条只升不降的线，比没有指标更误导人。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description BusinessController
 **/
@Slf4j
@RestController
@RequestMapping("/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessMetrics metrics;

    private final AtomicLong idGenerator = new AtomicLong();

    /**
     * 下单，累计成功计数并记录处理耗时。
     *
     * @param amount 订单金额
     * @return 订单号与耗时
     */
    @PostMapping("/orders")
    public Result<Map<String, Object>> create(@RequestParam(defaultValue = "100") long amount) {
        long start = System.currentTimeMillis();
        metrics.enter();
        try {
            long id = idGenerator.incrementAndGet();
            metrics.orderCreated();
            metrics.recordHandle(System.currentTimeMillis() - start, "success");

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("orderId", id);
            data.put("amount", amount);
            data.put("costMs", System.currentTimeMillis() - start);
            return Result.ok(data);
        } finally {
            metrics.leave();
        }
    }

    /**
     * 故意拖慢的请求，给 Timer 制造大样本撑 P95。
     *
     * @param ms 睡眠毫秒数
     * @return 提示去哪个指标看结果
     */
    @GetMapping("/slow")
    public Result<Map<String, Object>> slow(@RequestParam(defaultValue = "200") long ms) throws InterruptedException {
        long start = System.currentTimeMillis();
        Thread.sleep(ms);
        metrics.recordHandle(System.currentTimeMillis() - start, "success");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sleepMs", ms);
        data.put("note", "到 /actuator/metrics/business.order.handle 看 P95 与最大值");
        return Result.ok(data);
    }

    /**
     * 模拟下单失败，按原因打点。
     *
     * @param reason 失败原因
     * @return 提示去哪个指标看结果
     */
    @GetMapping("/fail")
    public Result<Map<String, Object>> fail(@RequestParam(defaultValue = "stock_empty") String reason) {
        long start = System.currentTimeMillis();
        metrics.orderFailed(reason);
        metrics.recordHandle(System.currentTimeMillis() - start, "failure");
        log.warn("模拟下单失败，原因：{}", reason);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reason", reason);
        data.put("note", "到 /actuator/metrics/business.orders.failed 看各原因的计数");
        return Result.ok(data);
    }

    /**
     * 模块导航。
     *
     * @return 各监控端点路径
     */
    @GetMapping("/hello")
    public Result<Map<String, Object>> hello() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("module", "12-actuator");
        data.put("health", "/actuator/health");
        data.put("metrics", "/actuator/metrics");
        data.put("prometheus", "/actuator/prometheus");
        return Result.ok(data);
    }
}
