package top.zff06.mybatis.pool;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.zff06.mybatis.service.UserService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 练习四专用：把连接池上限压到 1，复现 Connection is not available
 * <p>
 * 三个关键点：
 * 1. properties 里显式指定 spring.datasource.type=HikariDataSource。
 * 因为项目里引入了 druid-spring-boot-4-starter，而它的自动配置条件是
 * @ConditionalOnProperty(spring.datasource.type = DruidDataSource, matchIfMissing = true)，
 * 也就是“不配 type 时 Druid 也会接管”。不写 type 的话跑出来的是 Druid 的报错文案
 * （wait millis 3000, active 1, maxActive 1），不是本练习要的 Hikari 文案。
 * 2. 连接池参数在这个测试类里独立指定，不去改 application.yml，所以跑测试不影响平时启动的环境。
 * 3. 用例是可重复执行的：@Sql 每个方法前重置 t_user（本地 MySQL 真实写库，和项目其它测试一致）。
 */
@SpringBootTest(properties = {
        "spring.datasource.type=com.zaxxer.hikari.HikariDataSource",
        "spring.datasource.hikari.pool-name=pool-demo",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.datasource.hikari.minimum-idle=1",
        "spring.datasource.hikari.connection-timeout=1500"
})
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"})
class ConnectionPoolExhaustTest {

    /** 每个线程占住连接不放的时长，要大于 connection-timeout(1500ms) */
    private static final long HOLD_MILLIS = 1000;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserService userService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("环境自检：当前确实是 Hikari 且池上限为 1")
    void pool_shouldBeHikariWithSizeOne() {
        assertInstanceOf(HikariDataSource.class, dataSource,
                "必须在 Hikari 环境下跑，否则 druid starter 会接管数据源，报错文案对不上");

        HikariDataSource hikari = (HikariDataSource) dataSource;
        assertEquals(1, hikari.getMaximumPoolSize(), "池上限要是 1");
        assertEquals(1500, hikari.getConnectionTimeout(), "取连接超时要是 1500ms");

        System.out.printf("[自检] 连接池=%s 上限=%d 取连接超时=%dms%n",
                hikari.getPoolName(), hikari.getMaximumPoolSize(), hikari.getConnectionTimeout());
    }

    @Test
    @DisplayName("占住唯一的连接，第二个请求取连接超时：Connection is not available")
    void secondRequest_shouldFail_whenSingleConnectionHeld() throws Exception {
        long cost;
        Exception error;
        try (Connection held = dataSource.getConnection()) {
            System.out.println("[步骤1] 已从池中借走唯一一个连接，池内可用连接 = 0");

            long start = System.currentTimeMillis();
            // 这一步会去池里取连接，等满 connection-timeout 后抛异常
            error = org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> userService.list());
            cost = System.currentTimeMillis() - start;

            System.out.printf("[步骤2] userService.list() 抛异常，耗时 %dms%n", cost);
            System.out.println("[步骤2] 异常链：" + describe(error));
        }

        String message = describe(error);
        assertTrue(message.contains("Connection is not available"),
                "报错里应该出现 Connection is not available，实际为：" + message);
        assertTrue(cost >= 1400,
                "应该是在 connection-timeout(1500ms) 之后才失败，实际耗时 " + cost + "ms");

        // 连接已归还，池恢复可用
        assertFalse(userService.list().isEmpty(), "连接归还后应该立刻恢复正常");
        System.out.println("[步骤3] 连接归还，接口恢复正常");
    }

    @Test
    @DisplayName("8 个并发事务抢 1 个连接：部分成功、其余全部取连接超时")
    void concurrentTransactions_shouldPartiallyFail() throws Exception {
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 1; i <= threads; i++) {
            int no = i;
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    // 事务不提交，连接不会被归还
                    transactionTemplate.execute(status -> {
                        userService.list();
                        sleep(HOLD_MILLIS);
                        return null;
                    });
                    return "#" + no + " OK";
                } catch (Exception e) {
                    return "#" + no + " FAIL | " + describe(e);
                }
            }));
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS), "并发线程未就绪");
        long begin = System.currentTimeMillis();
        start.countDown();

        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            results.add(future.get(30, TimeUnit.SECONDS));
        }
        long total = System.currentTimeMillis() - begin;
        executor.shutdownNow();

        List<String> failures = results.stream().filter(r -> r.contains("FAIL")).toList();
        System.out.printf("[结果] 8 个并发事务共耗时 %dms，成功 %d 个，失败 %d 个%n",
                total, results.size() - failures.size(), failures.size());
        results.forEach(System.out::println);

        assertTrue(results.size() - failures.size() >= 1, "池里至少能跑完 1 个事务");
        assertFalse(failures.isEmpty(), "池上限为 1、并发 8 个，必须出现取连接失败");
        failures.forEach(f -> assertTrue(f.contains("Connection is not available"),
                "失败原因应该是取连接超时，实际为：" + f));
    }

    private static String describe(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (Throwable t = e; t != null; t = t.getCause()) {
            sb.append(t.getClass().getSimpleName()).append(": ").append(t.getMessage()).append(" <- ");
        }
        return sb.toString();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
