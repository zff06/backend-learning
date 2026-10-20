package top.zff06.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 随堂练习测试：文档 8.1 节五道练习的参考答案
 * <p>
 * 只依赖 Redis，不碰数据库，所以不需要重置 t_goods。
 */
@SpringBootTest
class PracticeTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("练习 1：用 Hash 只改一个字段，其余字段保持不变")
    void practice1UpdateSingleHashField() {
        String key = "practice:goods:hash:1";
        stringRedisTemplate.delete(key);

        stringRedisTemplate.opsForHash().putAll(key, Map.of(
                "name", "机械键盘 K87", "category", "数码", "price", "399.00"));

        // put 只覆盖指定字段，putAll 会覆盖同名字段、保留其余字段
        stringRedisTemplate.opsForHash().put(key, "price", "359.00");

        Map<Object, Object> fields = stringRedisTemplate.opsForHash().entries(key);
        assertThat(fields).containsEntry("price", "359.00")
                .containsEntry("name", "机械键盘 K87")
                .containsEntry("category", "数码");

        stringRedisTemplate.delete(key);
    }

    @Test
    @DisplayName("练习 2：Set 求交集，得到共同关注的商品")
    void practice2IntersectLikes() {
        stringRedisTemplate.opsForSet().add("practice:like:u1", "1", "2", "3");
        stringRedisTemplate.opsForSet().add("practice:like:u2", "2", "3", "4");

        Set<String> common = stringRedisTemplate.opsForSet()
                .intersect("practice:like:u1", "practice:like:u2");

        assertThat(common).containsExactlyInAnyOrder("2", "3");

        stringRedisTemplate.delete(List.of("practice:like:u1", "practice:like:u2"));
    }

    @Test
    @DisplayName("练习 3：TTL 抖动后，多个 key 的剩余时间不会完全一致")
    void practice3TtlJitter() {
        List<String> keys = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            String key = "practice:jitter:" + i;
            redisTemplate.opsForValue().set(key, "v",
                    Duration.ofMinutes(10).plusSeconds(ThreadLocalRandom.current().nextInt(31)));
            keys.add(key);
        }

        Set<Long> ttls = keys.stream()
                .map(redisTemplate::getExpire)
                .collect(Collectors.toSet());

        // 10 个 key 的 TTL 至少出现两种取值，说明抖动确实生效了
        assertThat(ttls).hasSizeGreaterThan(1);

        redisTemplate.delete(keys);
    }

    @Test
    @DisplayName("练习 4：SETNX 做提交去重，第二次被挡住")
    void practice4SubmitOnce() {
        String key = "practice:submit:1001";
        stringRedisTemplate.delete(key);

        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(5));
        Boolean second = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(5));

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(stringRedisTemplate.getExpire(key)).isPositive();

        stringRedisTemplate.delete(key);
    }

    @Test
    @DisplayName("练习 5：取某人前后各一名")
    void practice5NeighborRank() {
        String key = "practice:rank";
        stringRedisTemplate.delete(key);

        // 分数 100 ~ 500，倒序后 m5 第 1 名、m1 第 5 名
        for (int i = 1; i <= 5; i++) {
            stringRedisTemplate.opsForZSet().add(key, "m" + i, i * 100.0);
        }

        // reverseRank 从 0 开始，所以 m3 是 2
        Long rank = stringRedisTemplate.opsForZSet().reverseRank(key, "m3");
        assertThat(rank).isEqualTo(2L);

        // 边界要夹到 0，否则第一名会取到 -1
        int center = rank.intValue();
        Set<ZSetOperations.TypedTuple<String>> window = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(key, Math.max(0, center - 1), center + 1L);

        assertThat(window).extracting(ZSetOperations.TypedTuple::getValue)
                .containsExactly("m4", "m3", "m2");

        // 榜首取prev时不会越界
        Long topRank = stringRedisTemplate.opsForZSet().reverseRank(key, "m5");
        Set<ZSetOperations.TypedTuple<String>> topWindow = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(key, Math.max(0, topRank.intValue() - 1), topRank + 1L);
        assertThat(topWindow).extracting(ZSetOperations.TypedTuple::getValue)
                .containsExactly("m5", "m4");

        stringRedisTemplate.delete(key);
    }
}
