package top.zff06.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import top.zff06.redis.controller.LockController;
import top.zff06.redis.service.GoodsService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 并发扣库存、排行榜与限流测试：覆盖文档 6.4、6.5 节的 11 个接口
 * <p>
 * 并发用例必须真并发：单线程顺序执行，不加锁也不会出错，用 CountDownLatch 让 20 个线程
 * 在同一时刻进入方法，丢失更新与锁的差别才会显出来。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"})
class LockAndLimitTest {

    /** 并发线程数，与文档 6.4 节一致 */
    private static final int THREADS = 20;

    /** 每轮实验的初始库存 */
    private static final int INITIAL_STOCK = 10;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LockController lockController;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void resetRedisKeys() {
        deleteKeys("lock:goods:*");
        deleteKeys("rank:*");
        deleteKeys("limit:access:*");
    }

    /* ---------- 库存基础操作 ---------- */

    @Test
    @DisplayName("重置库存与查库存：查的是数据库里的真实值")
    void resetAndStockShouldReflectDbValue() throws Exception {
        mockMvc.perform(post("/lock/reset").param("id", "1").param("stock", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stock").value(10));

        mockMvc.perform(get("/lock/stock/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stock").value(10));

        assertThat(goodsService.getFromDb(1L).getStock()).isEqualTo(10);
    }

    /* ---------- 不加锁 ---------- */

    @Test
    @DisplayName("不加锁：单发请求正常扣减")
    void buyWithoutLockShouldSucceedOnce() throws Exception {
        lockController.reset(1L, INITIAL_STOCK);

        mockMvc.perform(post("/lock/buy/no-lock").param("id", "1").param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("no-lock"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.stockLeft").value(INITIAL_STOCK - 1));
    }

    @Test
    @DisplayName("不加锁：20 个请求全部返回成功，库存却减不下去（丢失更新）")
    void buyWithoutLockShouldLoseUpdate() throws Exception {
        lockController.reset(1L, INITIAL_STOCK);

        List<Map<String, Object>> results = callConcurrently(THREADS, () -> lockController.buyWithoutLock(1L, 1));

        long successCount = results.stream().filter(r -> Boolean.TRUE.equals(r.get("success"))).count();
        int stockLeft = goodsService.getFromDb(1L).getStock();

        // 账面上卖出 successCount 件，实际只扣掉 INITIAL_STOCK - stockLeft 件，差额就是被覆盖掉的更新
        assertThat(successCount).isEqualTo(THREADS);
        assertThat(stockLeft).isPositive();
        assertThat(INITIAL_STOCK - stockLeft).isLessThan((int) successCount);
    }

    /* ---------- Redisson ---------- */

    @Test
    @DisplayName("Redisson：20 个请求抢 10 件库存，恰好卖出 10 件")
    void buyWithRedissonShouldNotOversell() throws Exception {
        lockController.reset(1L, INITIAL_STOCK);

        List<Map<String, Object>> results = callConcurrently(THREADS, () -> lockController.buyWithRedisson(1L, 1));

        long successCount = results.stream().filter(r -> Boolean.TRUE.equals(r.get("success"))).count();
        long failedCount = results.stream().filter(r -> Boolean.FALSE.equals(r.get("success"))).count();

        // 锁把请求排成一队，前 10 件卖出、后面 10 件进临界区时库存已空
        assertThat(successCount).isEqualTo(INITIAL_STOCK);
        assertThat(failedCount).isEqualTo(THREADS - INITIAL_STOCK);
        assertThat(goodsService.getFromDb(1L).getStock()).isZero();
    }

    @Test
    @DisplayName("Redisson：库存不够时不扣减，返回失败并带上剩余库存")
    void buyWithRedissonShouldRejectWhenStockNotEnough() throws Exception {
        lockController.reset(1L, 5);

        mockMvc.perform(post("/lock/buy/redisson").param("id", "1").param("quantity", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("redisson"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.stockLeft").value(5));
    }

    /* ---------- 手写 SETNX ---------- */

    @Test
    @DisplayName("手写 SETNX：抢不到锁立即返回失败，不做等待")
    void buyWithSetNxShouldRefuseInsteadOfWaiting() throws Exception {
        lockController.reset(1L, INITIAL_STOCK);

        List<Map<String, Object>> results = callConcurrently(THREADS, () -> lockController.buyWithSetNx(1L, 1));

        long successCount = results.stream().filter(r -> Boolean.TRUE.equals(r.get("success"))).count();
        long refusedCount = results.stream()
                .filter(r -> "没抢到锁，已有请求在处理".equals(r.get("reason")))
                .count();

        // 两条出路：抢到锁执行完、或者没抢到被直接拒绝，不会有第三种
        assertThat(successCount + refusedCount).isEqualTo(THREADS);
        assertThat(successCount).isLessThanOrEqualTo(INITIAL_STOCK);
        // 每个成功扣 1 件，所以库存正好是初始值减去成功次数
        assertThat(goodsService.getFromDb(1L).getStock()).isEqualTo(INITIAL_STOCK - (int) successCount);
    }

    /* ---------- 锁状态 ---------- */

    @Test
    @DisplayName("查锁：释放后 TTL 为 -2，手动放的带过期 key 能看到真实秒数")
    void lockInfoShouldReturnTtl() throws Exception {
        lockController.buyWithSetNx(1L, 1);

        mockMvc.perform(get("/lock/info").param("key", "lock:goods:setnx:1"))
                .andExpect(jsonPath("$.key").value("lock:goods:setnx:1"))
                .andExpect(jsonPath("$.ttlSeconds").value(-2));

        stringRedisTemplate.opsForValue().set("lock:goods:demo", "1", Duration.ofSeconds(30));
        mockMvc.perform(get("/lock/info").param("key", "lock:goods:demo"))
                .andExpect(jsonPath("$.ttlSeconds").value(30));
    }

    /* ---------- 排行榜 ---------- */

    @Test
    @DisplayName("排行榜：预热把 10 条商品的销量灌进 ZSet，取前 3 名按分数倒序")
    void rankWarmUpShouldLoadAllGoodsAndOrderByScore() throws Exception {
        mockMvc.perform(post("/rank/warmup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warmed").value(10))
                .andExpect(jsonPath("$.size").value(10));

        mockMvc.perform(get("/rank/top").param("top", "3"))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].goodsId").value("6"))
                .andExpect(jsonPath("$[0].sales").value(4300.0))
                .andExpect(jsonPath("$[1].rank").value(2))
                // name 是拿 ZSet 里的 goodsId 回查数据库补上的
                .andExpect(jsonPath("$[0].name").isNotEmpty());
    }

    @Test
    @DisplayName("排行榜：加分后名次自动变化，没上过榜的 id 返回 -1")
    void rankIncreaseShouldUpdatePosition() throws Exception {
        mockMvc.perform(post("/rank/warmup"));

        // 7 号原来 275 分，加 3000 后是 3275，直接进第 2 名
        mockMvc.perform(post("/rank/increase").param("id", "7").param("delta", "3000"))
                .andExpect(jsonPath("$.goodsId").value(7))
                .andExpect(jsonPath("$.score").value(3275.0))
                .andExpect(jsonPath("$.rank").value(2));

        mockMvc.perform(get("/rank/rank-of").param("id", "7"))
                .andExpect(jsonPath("$.rank").value(2))
                .andExpect(jsonPath("$.sales").value(3275.0));

        mockMvc.perform(get("/rank/rank-of").param("id", "999"))
                .andExpect(jsonPath("$.rank").value(-1))
                .andExpect(jsonPath("$.sales").doesNotExist());
    }

    /* ---------- 限流 ---------- */

    @Test
    @DisplayName("限流：同一用户前 5 次放行、第 6 次起被拒，换用户重新计数")
    void limitShouldRejectAfterMaxRequestsAndIsolateByUser() throws Exception {
        // 用带时间戳的用户名，避免上一次运行留下的窗口计数影响断言
        String user = "test-limit-" + System.nanoTime();

        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(get("/limit/access").param("user", user))
                    .andExpect(jsonPath("$.count").value(i))
                    .andExpect(jsonPath("$.maxRequests").value(5))
                    .andExpect(jsonPath("$.windowSeconds").value(60))
                    .andExpect(jsonPath("$.allowed").value(true));
        }

        mockMvc.perform(get("/limit/access").param("user", user))
                .andExpect(jsonPath("$.count").value(6))
                .andExpect(jsonPath("$.allowed").value(false));

        // 计数器按用户隔离，换个用户从 1 重新开始
        mockMvc.perform(get("/limit/access").param("user", user + "-other"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.allowed").value(true));
    }

    /* ---------- 工具方法 ---------- */

    /** 用 CountDownLatch 让所有线程在同一时刻进入方法 */
    private List<Map<String, Object>> callConcurrently(int threads, Supplier<Map<String, Object>> action)
            throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Map<String, Object>>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return action.get();
                }));
            }
            start.countDown();

            List<Map<String, Object>> results = new ArrayList<>();
            for (Future<Map<String, Object>> future : futures) {
                results.add(future.get());
            }
            return results;
        } catch (ExecutionException e) {
            throw new IllegalStateException("并发扣库存调用失败", e.getCause());
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
