package top.zff06.redis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.redis.config.DemoProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @auther zff06
 * @data 2026/10/14
 * @description 计数器做固定窗口限流：把时间按窗口长度切片，每片一个计数器，key 里带上窗口编号
 **/
@RestController
@RequestMapping("/limit")
@RequiredArgsConstructor
public class LimitController {

    private final StringRedisTemplate stringRedisTemplate;
    private final DemoProperties properties;

    /**
     * 访问一次。
     * 计数器只负责数，不负责拦：allowed 为 false 时由业务代码返回 429。
     */
    @GetMapping("/access")
    public Map<String, Object> access(@RequestParam(defaultValue = "guest") String user) {
        int windowSeconds = properties.getLimit().getWindowSeconds();
        int maxRequests = properties.getLimit().getMaxRequests();

        long windowIndex = System.currentTimeMillis() / 1000 / windowSeconds;
        String key = "limit:access:" + user + ":" + windowIndex;

        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            // 只在计数器刚建出来时设过期，之后每次自增不再刷新，保证窗口按自然时间滚动；
            // 每次自增都刷新的话，只要请求不断计数器就永不过期，限流会退化成"总共只放行 5 次"
            stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", user);
        result.put("count", count);
        result.put("maxRequests", maxRequests);
        result.put("windowSeconds", windowSeconds);
        result.put("allowed", count != null && count <= maxRequests);
        result.put("ttlSeconds", stringRedisTemplate.getExpire(key));
        return result;
    }
}
