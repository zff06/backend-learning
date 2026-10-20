package top.zff06.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 数据类型接口测试：逐个覆盖文档 6.2 节的 13 个接口
 * <p>
 * 用 MockMvc 直连本地 Redis（与 application.yml 同一套连接），不占用真实端口。
 * 每个用例开始前清掉 demo:* 前缀的 key，避免上一次运行的残留影响断言。
 */
@SpringBootTest
@AutoConfigureMockMvc
class RedisTypeOpsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void cleanDemoKeys() {
        Set<String> keys = stringRedisTemplate.keys("demo:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    /* ---------- String ---------- */

    @Test
    @DisplayName("String：写入返回剩余秒数，读回来是写入的值")
    void stringWriteAndRead() throws Exception {
        mockMvc.perform(post("/redis/string")
                        .param("key", "demo:name")
                        .param("value", "mqxu")
                        .param("ttlSeconds", "120"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("demo:name"))
                .andExpect(jsonPath("$.value").value("mqxu"))
                .andExpect(jsonPath("$.ttlSeconds", greaterThan(0)));

        mockMvc.perform(get("/redis/string").param("key", "demo:name"))
                .andExpect(status().isOk())
                .andExpect(content().string("mqxu"));

        // key 不存在时读到的是空响应体
        mockMvc.perform(get("/redis/string").param("key", "demo:nobody"))
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("ttl：正数是剩余秒数，-1 表示没设过期，-2 表示 key 不存在")
    void ttlThreeKindsOfReturnValue() throws Exception {
        mockMvc.perform(post("/redis/string")
                .param("key", "demo:name").param("value", "mqxu").param("ttlSeconds", "120"));
        mockMvc.perform(get("/redis/ttl").param("key", "demo:name"))
                .andExpect(jsonPath("$.key").value("demo:name"))
                .andExpect(jsonPath("$.ttlSeconds", greaterThan(0)));

        // 不带过期时间的 key
        stringRedisTemplate.opsForValue().set("demo:forever", "v");
        mockMvc.perform(get("/redis/ttl").param("key", "demo:forever"))
                .andExpect(jsonPath("$.ttlSeconds").value(-1));

        mockMvc.perform(get("/redis/ttl").param("key", "demo:nobody"))
                .andExpect(jsonPath("$.ttlSeconds").value(-2));
    }

    @Test
    @DisplayName("删 key：删掉返回 true，本来不存在返回 false，删完 TTL 变 -2")
    void deleteKeyReturnsTrueThenFalse() throws Exception {
        mockMvc.perform(post("/redis/string").param("key", "demo:name").param("value", "mqxu"));

        mockMvc.perform(delete("/redis/key").param("key", "demo:name"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        mockMvc.perform(delete("/redis/key").param("key", "nobody"))
                .andExpect(content().string("false"));

        mockMvc.perform(get("/redis/ttl").param("key", "demo:name"))
                .andExpect(jsonPath("$.ttlSeconds").value(-2));
    }

    /* ---------- Hash ---------- */

    @Test
    @DisplayName("Hash：字段名写在查询参数里，返回里不会出现 key 字段")
    void hashWriteShouldExcludeKeyField() throws Exception {
        mockMvc.perform(post("/redis/hash")
                        .param("key", "demo:user:1")
                        .param("name", "tao")
                        .param("role", "teacher")
                        .param("city", "nanjing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("tao"))
                .andExpect(jsonPath("$.role").value("teacher"))
                .andExpect(jsonPath("$.city").value("nanjing"))
                // @RequestParam Map 会收进 key 本身，代码里剔掉了，所以这里必须查不到
                .andExpect(jsonPath("$.key").doesNotExist());

        mockMvc.perform(get("/redis/hash").param("key", "demo:user:1"))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("Hash：同一个 key 再写一次是合并，原有字段保留")
    void hashWriteTwiceShouldMergeFields() throws Exception {
        mockMvc.perform(post("/redis/hash")
                        .param("key", "demo:user:2").param("name", "陶老师"))
                .andExpect(jsonPath("$.name").value("陶老师"))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(post("/redis/hash")
                        .param("key", "demo:user:2").param("city", "南京"))
                .andExpect(jsonPath("$.name").value("陶老师"))
                .andExpect(jsonPath("$.city").value("南京"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    /* ---------- List ---------- */

    @Test
    @DisplayName("List：rightPushAll 从右端追加，第二次写的排在末尾")
    void listPushShouldAppendToRight() throws Exception {
        mockMvc.perform(post("/redis/list")
                        .param("key", "demo:news").param("values", "a", "b", "c"))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("a"))
                .andExpect(jsonPath("$[2]").value("c"));

        mockMvc.perform(post("/redis/list")
                        .param("key", "demo:news").param("values", "d"))
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[3]").value("d"));

        mockMvc.perform(get("/redis/list").param("key", "demo:news"))
                .andExpect(content().json("[\"a\",\"b\",\"c\",\"d\"]"));
    }

    /* ---------- Set ---------- */

    @Test
    @DisplayName("Set：重复值不增加元素，交集取共同成员")
    void setShouldDeduplicateAndIntersect() throws Exception {
        mockMvc.perform(post("/redis/set")
                        .param("key", "demo:u1:like").param("values", "1", "2", "3", "3"))
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(post("/redis/set")
                        .param("key", "demo:u2:like").param("values", "2", "3", "4"))
                .andExpect(jsonPath("$.length()").value(3));

        assertThat(stringRedisTemplate.opsForSet().members("demo:u1:like"))
                .containsExactlyInAnyOrder("1", "2", "3");

        mockMvc.perform(get("/redis/set/intersect")
                        .param("key1", "demo:u1:like").param("key2", "demo:u2:like"))
                .andExpect(jsonPath("$.length()").value(2));

        assertThat(stringRedisTemplate.opsForSet().intersect("demo:u1:like", "demo:u2:like"))
                .containsExactlyInAnyOrder("2", "3");
    }

    /* ---------- ZSet ---------- */

    @Test
    @DisplayName("ZSet：按分数倒序取前 N，重复写同一 member 是覆盖分数")
    void zsetShouldOrderByScoreDesc() throws Exception {
        mockMvc.perform(post("/redis/zset")
                        .param("key", "demo:score").param("member", "bob").param("score", "95"))
                .andExpect(jsonPath("$.member").value("bob"))
                .andExpect(jsonPath("$.score").value(95.0))
                .andExpect(jsonPath("$.size").value(1));

        mockMvc.perform(post("/redis/zset")
                        .param("key", "demo:score").param("member", "alice").param("score", "88"))
                .andExpect(jsonPath("$.size").value(2));

        // 同一个 member 再写一次是覆盖，不是新增
        mockMvc.perform(post("/redis/zset")
                        .param("key", "demo:score").param("member", "alice").param("score", "97"))
                .andExpect(jsonPath("$.size").value(2));

        mockMvc.perform(get("/redis/zset/top").param("key", "demo:score").param("top", "3"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].member").value("alice"))
                .andExpect(jsonPath("$[0].score").value(97.0))
                .andExpect(jsonPath("$[1].member").value("bob"));
    }

    /* ---------- 计数器 ---------- */

    @Test
    @DisplayName("计数器：delta 缺省加 1，key 不存在时按 0 起算")
    void counterShouldAccumulate() throws Exception {
        mockMvc.perform(post("/redis/counter/incr")
                        .param("key", "demo:pv").param("delta", "5"))
                .andExpect(jsonPath("$.key").value("demo:pv"))
                .andExpect(jsonPath("$.current").value(5));

        mockMvc.perform(post("/redis/counter/incr").param("key", "demo:pv"))
                .andExpect(jsonPath("$.current").value(6));
    }
}
