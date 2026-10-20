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

/**
 * 练习四对照组：连接池上限调回正常值（10），同样的 8 个并发事务全部成功
 * <p>
 * 和 {@link ConnectionPoolExhaustTest} 唯一的差别就是 maximum-pool-size：
 * 1 → 并发失败；10 → 并发全过。这一组对比就是"连接池上限决定并发能力"的直接证据。
 */
@SpringBootTest(properties = {
        "spring.datasource.type=com.zaxxer.hikari.HikariDataSource",
        "spring.datasource.hikari.pool-name=pool-normal",
        "spring.datasource.hikari.maximum-pool-size=10",
        "spring.datasource.hikari.minimum-idle=2",
        "spring.datasource.hikari.connection-timeout=1500"
})
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"})
class ConnectionPoolDefaultSizeTest {

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
    @DisplayName("池上限 10：8 个并发事务全部成功，一个都不失败")
    void concurrentTransactions_shouldAllSucceed() throws Exception {
        assertInstanceOf(HikariDataSource.class, dataSource);
        assertEquals(10, ((HikariDataSource) dataSource).getMaximumPoolSize());

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
                    transactionTemplate.execute(status -> {
                        userService.list();
                        sleep(HOLD_MILLIS);
                        return null;
                    });
                    return "#" + no + " OK";
                } catch (Exception e) {
                    return "#" + no + " FAIL | " + e.getClass().getSimpleName() + ": " + e.getMessage();
                }
            }));
        }

        ready.await(5, TimeUnit.SECONDS);
        long begin = System.currentTimeMillis();
        start.countDown();

        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            results.add(future.get(30, TimeUnit.SECONDS));
        }
        long total = System.currentTimeMillis() - begin;
        executor.shutdownNow();

        List<String> failures = results.stream().filter(r -> r.contains("FAIL")).toList();
        System.out.printf("[对照] 池上限 10，8 个并发事务耗时 %dms，成功 %d 个，失败 %d 个%n",
                total, results.size() - failures.size(), failures.size());
        results.forEach(System.out::println);

        assertEquals(0, failures.size(), "池够用时不应该有任何失败：" + failures);
        assertFalse(userService.list().isEmpty());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
