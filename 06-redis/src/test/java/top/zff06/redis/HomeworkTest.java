package top.zff06.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.jdbc.Sql;
import top.zff06.redis.entity.Goods;
import top.zff06.redis.mapper.GoodsMapper;
import top.zff06.redis.service.GoodsService;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 课后练习测试：文档 8.2 节四道练习的参考答案
 * <p>
 * 练习 9 会改数据库里的价格，所以每个用例前都重跑 schema.sql + data.sql。
 */
@SpringBootTest
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"})
class HomeworkTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private GoodsMapper goodsMapper;

    @BeforeEach
    void cleanHomeworkKeys() {
        Set<String> keys = stringRedisTemplate.keys("homework:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("练习 6：购物车用 Hash 存，HINCRBY 让加购与减购都是原子操作")
    void homework6ShoppingCart() {
        String key = "homework:cart:1001";
        stringRedisTemplate.delete(key);

        stringRedisTemplate.opsForHash().increment(key, "1", 2);    // 商品 1 加 2 件
        stringRedisTemplate.opsForHash().increment(key, "1", 1);    // 再加 1 件
        stringRedisTemplate.opsForHash().increment(key, "2", 1);    // 商品 2 加 1 件
        stringRedisTemplate.opsForHash().increment(key, "1", -3);   // 商品 1 减 3 件

        Map<Object, Object> cart = stringRedisTemplate.opsForHash().entries(key);
        assertThat(cart).containsEntry("1", "0").containsEntry("2", "1");

        // 数量为 0 的条目由业务代码删掉
        stringRedisTemplate.opsForHash().delete(key, "1");
        assertThat(stringRedisTemplate.opsForHash().entries(key)).hasSize(1);

        stringRedisTemplate.delete(key);
    }

    @Test
    @DisplayName("练习 7：逻辑过期——过期后先返回旧值，只放一个线程去重建")
    void homework7LogicalExpire() {
        String key = "homework:logical:1";
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("data", "旧值");
        wrapper.put("expireAt", System.currentTimeMillis() - 1000);   // 已逻辑过期
        redisTemplate.opsForValue().set(key, wrapper, Duration.ofMinutes(30));  // 物理上不过期

        @SuppressWarnings("unchecked")
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(key);

        // 往 Map<String, Object> 里存的时间戳，反序列化后是 Number 的具体子类，用 longValue() 最稳
        long expireAt = ((Number) cached.get("expireAt")).longValue();
        String returned = (String) cached.get("data");

        AtomicInteger rebuildCount = new AtomicInteger();
        if (expireAt < System.currentTimeMillis()) {
            // 抢到锁的线程才重建，其余线程直接拿到旧值
            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key + ":rebuild", "1", Duration.ofSeconds(5));
            if (Boolean.TRUE.equals(locked)) {
                rebuildCount.incrementAndGet();
            }
        }

        assertThat(returned).isEqualTo("旧值");
        assertThat(rebuildCount.get()).isEqualTo(1);

        stringRedisTemplate.delete(List.of(key, key + ":rebuild"));
    }

    @Test
    @DisplayName("练习 8：按接口维度限流，并返回剩余可用次数")
    void homework8ApiLimitRemaining() {
        String api = "/orders";
        String user = "u1001";
        int windowSeconds = 60;
        int maxRequests = 3;
        long windowIndex = System.currentTimeMillis() / 1000 / windowSeconds;
        String key = "homework:limit:" + api + ":" + user + ":" + windowIndex;
        stringRedisTemplate.delete(key);

        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                // 只在计数器刚建出来时设过期，之后自增不再刷新，窗口才能按自然时间滚动
                stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("count", count);
            // maxRequests - count 是 long，所以 0 必须写成 0L，否则断言时 Long 与 Integer 不相等
            item.put("remaining", Math.max(0L, maxRequests - count));
            item.put("allowed", count != null && count <= maxRequests);
            results.add(item);
        }

        assertThat(results.get(2)).containsEntry("remaining", 0L).containsEntry("allowed", true);
        assertThat(results.get(3)).containsEntry("remaining", 0L).containsEntry("allowed", false);

        stringRedisTemplate.delete(key);
    }

    @Test
    @DisplayName("练习 9：先删缓存再更新库，中间进来的一次读会把旧值写回缓存")
    void homework9DeleteThenUpdateLeavesStaleCache() {
        String key = "homework:stale:1";
        Goods before = goodsService.getFromDb(1L);
        BigDecimal oldPrice = before.getPrice();

        redisTemplate.opsForValue().set(key, before, Duration.ofMinutes(10));   // 缓存里先放好旧值
        redisTemplate.delete(key);                                             // 先删缓存

        // 另一个请求在删缓存之后、更新库之前读到旧值并写回缓存
        Goods readInWindow = goodsService.getFromDb(1L);
        redisTemplate.opsForValue().set(key, readInWindow, Duration.ofMinutes(10));

        // 再更新库，此时缓存里留下的是旧值
        Goods update = new Goods();
        update.setId(1L);
        update.setPrice(oldPrice.add(new BigDecimal("10.00")));
        goodsMapper.updateById(update);

        Object cached = redisTemplate.opsForValue().get(key);
        assertThat(((Goods) cached).getPrice()).isEqualByComparingTo(oldPrice);
        assertThat(goodsService.getFromDb(1L).getPrice())
                .isEqualByComparingTo(oldPrice.add(new BigDecimal("10.00")));

        redisTemplate.delete(key);
    }
}
