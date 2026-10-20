package top.zff06.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import top.zff06.redis.controller.CacheController;
import top.zff06.redis.dto.CacheResult;
import top.zff06.redis.entity.Goods;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 缓存测试：覆盖文档 6.3 节的 10 个接口，三种做法（直查库 / 手动缓存 / 注解缓存）各验证一遍
 * <p>
 * 每个用例前重跑 schema.sql + data.sql 重置 t_goods（10 条商品），再清掉本模块用到的 key，
 * 保证用例之间互不干扰。判断缓存有没有生效主要看返回体里的 source 字段。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"})
class GoodsCacheTest {

    /** 并发重建缓存用的线程数，与文档 6.3 节一致 */
    private static final int THREADS = 10;

    /** 手动缓存的 key 前缀，形如 goods:detail:1 */
    private static final String DETAIL_PREFIX = "goods:detail:";

    /** 注解缓存的 key 前缀，形如 cache:goods::4 */
    private static final String ANNO_PREFIX = "cache:goods";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheController cacheController;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void resetCacheKeys() {
        deleteKeys(DETAIL_PREFIX + "*");
        deleteKeys(ANNO_PREFIX + "*");
        deleteKeys("lock:goods:rebuild:*");
    }

    /* ---------- 直查库 ---------- */

    @Test
    @DisplayName("直查库：连查两次都是 db，这条路径不碰缓存")
    void fromDbShouldAlwaysHitDatabase() throws Exception {
        mockMvc.perform(get("/cache/goods/db/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("db"))
                .andExpect(jsonPath("$.data.id").value(1));

        mockMvc.perform(get("/cache/goods/db/{id}", 1L))
                .andExpect(jsonPath("$.source").value("db"));

        assertThat(redisTemplate.hasKey(DETAIL_PREFIX + 1)).isFalse();
    }

    /* ---------- 手动缓存 ---------- */

    @Test
    @DisplayName("手动缓存：第一次 db，第二次 source 变 cache")
    void manualCacheShouldRebuildThenHitCache() throws Exception {
        // 缓存里本来没有这个 key，删了返回 false
        mockMvc.perform(delete("/cache/goods/manual/{id}/cache", 1L))
                .andExpect(content().string("false"));

        mockMvc.perform(get("/cache/goods/manual/{id}", 1L))
                .andExpect(jsonPath("$.source").value("db"))
                .andExpect(jsonPath("$.data.id").value(1));

        mockMvc.perform(get("/cache/goods/manual/{id}", 1L))
                .andExpect(jsonPath("$.source").value("cache"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("空值防穿透：查不存在的 id，第二次命中空值标记")
    void manualCacheShouldWriteNullMarker() throws Exception {
        mockMvc.perform(get("/cache/goods/manual/{id}", 999L))
                .andExpect(jsonPath("$.source").value("db"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/cache/goods/manual/{id}", 999L))
                .andExpect(jsonPath("$.source").value("cache-empty"))
                .andExpect(jsonPath("$.data").doesNotExist());

        // 缓存里留下的是字符串 NULL 标记，不是 Goods
        assertThat(redisTemplate.opsForValue().get(DETAIL_PREFIX + 999)).isEqualTo("NULL");
    }

    @Test
    @DisplayName("空值标记的 TTL 比正常值短，正常值带 0~30 秒抖动")
    void nullMarkerShouldHaveShorterTtl() throws Exception {
        mockMvc.perform(get("/cache/goods/manual/{id}", 999L));
        mockMvc.perform(get("/cache/goods/manual/{id}", 2L));

        // 空值标记：demo.cache.null-ttl = 2 分钟，不参与抖动
        Long nullTtl = redisTemplate.getExpire(DETAIL_PREFIX + 999);
        assertThat(nullTtl).isNotNull().isBetween(118L, 120L);

        // 正常值：demo.cache.ttl = 10 分钟 + 0~30 秒抖动
        Long normalTtl = redisTemplate.getExpire(DETAIL_PREFIX + 2);
        assertThat(normalTtl).isNotNull().isBetween(595L, 630L);

        assertThat(normalTtl).isGreaterThan(nullTtl);
    }

    @Test
    @DisplayName("手动缓存：读出来的值能还原成 Goods，不是 LinkedHashMap")
    void manualCacheValueShouldDeserializeBackToGoods() {
        cacheController.manual(1L);

        Object cached = redisTemplate.opsForValue().get(DETAIL_PREFIX + 1);

        // 打开 default typing 之后 JSON 里带 @class，才能还原成原本的类型；
        // 没配的话这里会是一个 LinkedHashMap，强转 Goods 会抛 ClassCastException
        assertThat(cached).isInstanceOf(Goods.class);
        assertThat(((Goods) cached).getId()).isEqualTo(1L);
        assertThat(((Goods) cached).getName()).isNotBlank();
    }

    /* ---------- 互斥锁防击穿 ---------- */

    @Test
    @DisplayName("互斥锁：单发请求先 db 后 cache，并在结束时释放重建锁")
    void manualMutexShouldRebuildThenHitCache() throws Exception {
        mockMvc.perform(get("/cache/goods/manual-mutex/{id}", 1L))
                .andExpect(jsonPath("$.source").value("db"));
        mockMvc.perform(get("/cache/goods/manual-mutex/{id}", 1L))
                .andExpect(jsonPath("$.source").value("cache"));

        // 锁是临时的，方法返回时已经删掉，TTL 为 -2
        assertThat(stringRedisTemplate.getExpire("lock:goods:rebuild:1")).isEqualTo(-2L);
    }

    @Test
    @DisplayName("互斥锁：清缓存后并发 10 个请求，只有一个回源查库")
    void manualMutexShouldQueryDbOnlyOnceUnderConcurrency() throws Exception {
        cacheController.evictManual(3L);

        List<CacheResult<Goods>> results = callConcurrently(THREADS, () -> cacheController.manualWithMutex(3L));

        long dbCount = results.stream().filter(r -> "db".equals(r.source())).count();
        assertThat(dbCount).isEqualTo(1);

        // 其余请求在等锁期间重读缓存，拿到的都是同一份数据
        assertThat(results).allSatisfy(r -> assertThat(r.data()).isNotNull());
    }

    /* ---------- 注解缓存 ---------- */

    @Test
    @DisplayName("注解缓存：清空区域后第一次回源，缓存里出现 cache:goods::4")
    void annoCacheShouldWriteKeyIntoRedis() throws Exception {
        mockMvc.perform(delete("/cache/goods/anno"))
                .andExpect(content().string("已清空 goods 缓存区域"));

        mockMvc.perform(get("/cache/goods/anno/{id}", 4L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.source").value("anno（是否命中看日志）"));

        assertThat(stringRedisTemplate.hasKey(ANNO_PREFIX + "::4")).isTrue();

        // 注解缓存的值同样是带 @class 的 JSON，能还原成 Goods
        Object cached = redisTemplate.opsForValue().get(ANNO_PREFIX + "::4");
        assertThat(cached).isInstanceOf(Goods.class);
        assertThat(((Goods) cached).getId()).isEqualTo(4L);
    }

    @Test
    @DisplayName("注解缓存防穿透：查不存在的 id 不抛异常，null 被写进缓存")
    void annoCacheShouldCacheNullValue() throws Exception {
        mockMvc.perform(delete("/cache/goods/anno"));

        mockMvc.perform(get("/cache/goods/anno/{id}", 77777L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/cache/goods/anno/{id}", 77777L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        // spring.cache.redis.cache-null-values=true + enableSpringCacheNullValueSupport()
        // 让 null 也能进缓存，第二次不会再打库
        assertThat(stringRedisTemplate.hasKey(ANNO_PREFIX + "::77777")).isTrue();
    }

    @Test
    @DisplayName("注解缓存列表：key 是固定的 all，返回全部 10 条商品")
    void annoListCacheShouldUseFixedAllKey() throws Exception {
        mockMvc.perform(delete("/cache/goods/anno"));

        mockMvc.perform(get("/cache/goods/anno/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(10))
                .andExpect(jsonPath("$.source").value("anno（是否命中看日志）"));

        assertThat(stringRedisTemplate.hasKey(ANNO_PREFIX + "::all")).isTrue();
    }

    /* ---------- 改价 ---------- */

    @Test
    @DisplayName("改价：先更新库再删缓存，缓存里不会留下旧值")
    void updatePriceShouldUpdateDbAndEvictCache() throws Exception {
        // 先让缓存里有旧值，5 号（智能手环 5 代）在 data.sql 里的价格是 249.00
        cacheController.manual(5L);
        Object stale = redisTemplate.opsForValue().get(DETAIL_PREFIX + 5);
        assertThat(stale).isInstanceOf(Goods.class);
        assertThat(((Goods) stale).getPrice()).isEqualByComparingTo("249.00");

        mockMvc.perform(put("/cache/goods/{id}/price", 5L).param("price", "199.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("db"))
                .andExpect(jsonPath("$.data.price").value(199.00));

        // 更新库后删缓存，紧接着重新读一次并回填，所以缓存里是刚写进去的新值而不是旧值
        Object cached = redisTemplate.opsForValue().get(DETAIL_PREFIX + 5);
        assertThat(cached).isInstanceOf(Goods.class);
        assertThat(((Goods) cached).getPrice()).isEqualByComparingTo("199.00");
    }

    @Test
    @DisplayName("改价：注解版用 @CachePut 把新值写回缓存")
    void updatePriceAnnoShouldWriteBackWithCachePut() throws Exception {
        mockMvc.perform(delete("/cache/goods/anno"));

        mockMvc.perform(put("/cache/goods/{id}/price-anno", 6L).param("price", "59.90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("anno-put"))
                .andExpect(jsonPath("$.data.price").value(59.90));

        Object cached = redisTemplate.opsForValue().get(ANNO_PREFIX + "::6");
        assertThat(cached).isInstanceOf(Goods.class);
        assertThat(((Goods) cached).getPrice()).isEqualByComparingTo("59.90");
    }

    /* ---------- 三种清缓存 ---------- */

    @Test
    @DisplayName("清缓存：手动清单个 key 返回 true/false；注解清明细连带列表；清区域清空全部")
    void evictShouldClearDetailAndList() throws Exception {
        // 手动缓存：删掉返回 true，再删返回 false
        cacheController.manual(1L);
        mockMvc.perform(delete("/cache/goods/manual/{id}/cache", 1L))
                .andExpect(content().string("true"));
        mockMvc.perform(delete("/cache/goods/manual/{id}/cache", 1L))
                .andExpect(content().string("false"));

        // 注解缓存：先灌进明细与列表
        mockMvc.perform(get("/cache/goods/anno/{id}", 4L));
        mockMvc.perform(get("/cache/goods/anno/list"));
        assertThat(stringRedisTemplate.hasKey(ANNO_PREFIX + "::4")).isTrue();
        assertThat(stringRedisTemplate.hasKey(ANNO_PREFIX + "::all")).isTrue();

        // 只清明细会把列表一起清掉，因为列表里也含这个商品
        mockMvc.perform(delete("/cache/goods/anno/{id}", 4L))
                .andExpect(content().string("已清掉 goods 区域下 id=4 与 all 两处缓存"));
        assertThat(stringRedisTemplate.hasKey(ANNO_PREFIX + "::4")).isFalse();
        assertThat(stringRedisTemplate.hasKey(ANNO_PREFIX + "::all")).isFalse();

        // 清空整个区域
        mockMvc.perform(get("/cache/goods/anno/{id}", 2L));
        mockMvc.perform(delete("/cache/goods/anno"))
                .andExpect(content().string("已清空 goods 缓存区域"));
        assertThat(stringRedisTemplate.keys(ANNO_PREFIX + "*")).isNullOrEmpty();
    }

    /* ---------- 工具方法 ---------- */

    /** 用 CountDownLatch 让所有线程在同一时刻进入方法，单线程顺序执行复现不出并发问题 */
    private List<CacheResult<Goods>> callConcurrently(int threads, Supplier<CacheResult<Goods>> action)
            throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<CacheResult<Goods>>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return action.get();
                }));
            }
            start.countDown();

            List<CacheResult<Goods>> results = new ArrayList<>();
            for (Future<CacheResult<Goods>> future : futures) {
                results.add(future.get());
            }
            return results;
        } catch (ExecutionException e) {
            throw new IllegalStateException("并发调用缓存接口失败", e.getCause());
        } finally {
            pool.shutdownNow();
        }
    }

    private void deleteKeys(String pattern) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }
}
