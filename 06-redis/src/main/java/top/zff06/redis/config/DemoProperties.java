package top.zff06.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @auther zff06
 * @data 2026/10/14
 * @description 本模块的业务参数，对应 application.yml 里的 demo.*
 **/
@Data
@ConfigurationProperties(prefix = "demo")
public class DemoProperties {

    /** 缓存相关 */
    private Cache cache = new Cache();

    /** 分布式锁相关 */
    private Lock lock = new Lock();

    /** 限流相关 */
    private Limit limit = new Limit();

    @Data
    public static class Cache {
        /** 手动缓存的默认存活时间，对应 demo.cache.ttl */
        private Duration ttl = Duration.ofMinutes(10);

        /** 空值标记的存活时间，比正常值短，对应 demo.cache.null-ttl */
        private Duration nullTtl = Duration.ofMinutes(2);

        /** TTL 随机抖动幅度（秒），对应 demo.cache.jitter-seconds */
        private int jitterSeconds = 30;
    }

    @Data
    public static class Lock {
        /** 抢锁的最长等待时间（秒），对应 demo.lock.wait-seconds */
        private long waitSeconds = 3;

        /** 拿到锁后的自动释放时间（秒），对应 demo.lock.lease-seconds */
        private long leaseSeconds = 10;
    }

    @Data
    public static class Limit {
        /** 限流窗口长度（秒），对应 demo.limit.window-seconds */
        private int windowSeconds = 60;

        /** 窗口内允许的请求数，对应 demo.limit.max-requests */
        private int maxRequests = 5;
    }
}
