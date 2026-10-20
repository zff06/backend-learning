package top.zff06.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import top.zff06.redis.config.DemoProperties;
import top.zff06.redis.dto.CacheResult;
import top.zff06.redis.entity.Goods;
import top.zff06.redis.mapper.GoodsMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @auther zff06
 * @data 2026/10/14
 * @description Cache-Aside 手动维护缓存，三处防护都在这里：空值防穿透、互斥锁防击穿、TTL 抖动防雪崩
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsCacheService {

    /** 手动缓存的 key 前缀，最终形如 goods:detail:1 */
    public static final String KEY_PREFIX = "goods:detail:";

    /** 重建缓存用的互斥锁前缀 */
    private static final String REBUILD_LOCK_PREFIX = "lock:goods:rebuild:";

    /** 空值标记：库里查不到时把它写进缓存，避免同一个不存在的 id 反复打库 */
    private static final String NULL_PLACEHOLDER = "NULL";

    /** 重建锁的存活时间，比一次回源查询长一点即可 */
    private static final Duration REBUILD_LOCK_TTL = Duration.ofSeconds(5);

    /** 没抢到锁时等一小会儿再重读缓存的时长（毫秒） */
    private static final long RETRY_SLEEP_MS = 80;

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final GoodsMapper goodsMapper;
    private final DemoProperties properties;

    /**
     * 先查缓存，未命中再查库并回填。
     * 回填时顺带处理两种情况：查不到写空值标记、正常值带抖动 TTL。
     */
    public CacheResult<Goods> getById(Long id) {
        long start = System.nanoTime();
        String key = KEY_PREFIX + id;

        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return fromCache(cached, start);
        }

        Goods goods = goodsMapper.selectById(id);
        cacheIt(key, goods);
        return new CacheResult<>(goods, "db", CacheResult.costFrom(start));
    }

    /**
     * 互斥锁防击穿：热点 key 刚失效时可能同时进来几十个请求，
     * 只放抢到锁的那一个回源，其余请求短等后重读缓存。
     */
    public CacheResult<Goods> getByIdWithMutex(Long id) {
        long start = System.nanoTime();
        String key = KEY_PREFIX + id;

        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return fromCache(cached, start);
        }

        String lockKey = REBUILD_LOCK_PREFIX + id;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", REBUILD_LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            // 没抢到锁，说明有别的请求正在重建，等一小会儿再读缓存
            sleepQuietly(RETRY_SLEEP_MS);
            Object retried = redisTemplate.opsForValue().get(key);
            if (retried != null) {
                return fromCache(retried, start);
            }
            return new CacheResult<>(goodsMapper.selectById(id), "db-fallback", CacheResult.costFrom(start));
        }

        try {
            // 拿到锁后再复查一次：可能上一个持锁线程刚好写完缓存
            Object checked = redisTemplate.opsForValue().get(key);
            if (checked != null) {
                return fromCache(checked, start);
            }
            Goods goods = goodsMapper.selectById(id);
            cacheIt(key, goods);
            return new CacheResult<>(goods, "db", CacheResult.costFrom(start));
        } finally {
            // 这里的锁只服务于重建，值固定是 "1"、不区分持有者，直接删即可；
            // 对外提供服务的业务锁必须先比对持有者再删，见 StockService
            stringRedisTemplate.delete(lockKey);
        }
    }

    /** 更新价格：先更新库，再删缓存（三种更新策略里出现不一致概率最小的一种） */
    public boolean updatePrice(Long id, BigDecimal price) {
        Goods goods = new Goods();
        goods.setId(id);
        goods.setPrice(price);
        boolean updated = goodsMapper.updateById(goods) > 0;
        if (updated) {
            evict(id);
        }
        return updated;
    }

    /** 删掉单个商品的缓存，返回 true 表示确实删掉了、false 表示本来就不存在 */
    public boolean evict(Long id) {
        return Boolean.TRUE.equals(redisTemplate.delete(KEY_PREFIX + id));
    }

    /** 把查库结果写进缓存 */
    private void cacheIt(String key, Goods goods) {
        if (goods == null) {
            // 查不到也写一条标记，避免同一个不存在的 id 反复打库
            redisTemplate.opsForValue().set(key, NULL_PLACEHOLDER, properties.getCache().getNullTtl());
            return;
        }
        redisTemplate.opsForValue().set(key, goods, ttlWithJitter());
    }

    /** 基础 TTL 加上 0 到 jitterSeconds 秒的随机值，避免大批 key 在同一时刻集中过期 */
    private Duration ttlWithJitter() {
        long base = properties.getCache().getTtl().toSeconds();
        long jitter = ThreadLocalRandom.current().nextLong(properties.getCache().getJitterSeconds() + 1L);
        return Duration.ofSeconds(base + jitter);
    }

    /** 缓存里读出来的值有三种可能：空值标记、正常对象、类型不对 */
    private CacheResult<Goods> fromCache(Object cached, long start) {
        if (NULL_PLACEHOLDER.equals(cached)) {
            return new CacheResult<>(null, "cache-empty", CacheResult.costFrom(start));
        }
        if (cached instanceof Goods goods) {
            return new CacheResult<>(goods, "cache", CacheResult.costFrom(start));
        }
        log.warn("缓存里的值不是 Goods，实际是 {}，检查 RedisConfig 的序列化配置", cached.getClass().getName());
        return new CacheResult<>(null, "cache-invalid", CacheResult.costFrom(start));
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
