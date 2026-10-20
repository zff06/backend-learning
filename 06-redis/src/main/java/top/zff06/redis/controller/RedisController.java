package top.zff06.redis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @auther zff06
 * @data 2026/10/14
 * @description 五种数据类型与计数器的直接操作，只用 StringRedisTemplate，演示的都是字符串形式的简单值
 **/
@RestController
@RequestMapping("/redis")
@RequiredArgsConstructor
public class RedisController {

    private final StringRedisTemplate stringRedisTemplate;

    /** String：最基础的类型，文本、数字、序列化后的 JSON 都放它里面 */
    @PostMapping("/string")
    public Map<String, Object> setString(@RequestParam String key,
                                         @RequestParam String value,
                                         @RequestParam(defaultValue = "600") long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);
        result.put("value", value);
        result.put("ttlSeconds", stringRedisTemplate.getExpire(key));
        return result;
    }

    /** 读 String，key 不存在时返回空响应体 */
    @GetMapping("/string")
    public String getString(@RequestParam String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /** 查剩余过期时间：正数是剩余秒数，-1 表示没设过期时间，-2 表示 key 不存在 */
    @GetMapping("/ttl")
    public Map<String, Object> ttl(@RequestParam String key) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);
        result.put("ttlSeconds", stringRedisTemplate.getExpire(key));
        return result;
    }

    /** 删 key：true 表示删掉了一个，false 表示本来就不存在 */
    @DeleteMapping("/key")
    public boolean deleteKey(@RequestParam String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.delete(key));
    }

    /** Hash：一个 key 下挂多个字段，适合只读写对象的某几个属性 */
    @PostMapping("/hash")
    public Map<Object, Object> putHash(@RequestParam String key, @RequestParam Map<String, String> fields) {
        // RequestParam 收到的 Map 里包含 key 这个参数本身，写进 hash 前先剔掉
        Map<String, String> values = new LinkedHashMap<>(fields);
        values.remove("key");
        stringRedisTemplate.opsForHash().putAll(key, values);
        return stringRedisTemplate.opsForHash().entries(key);
    }

    /** 读整个 Hash */
    @GetMapping("/hash")
    public Map<Object, Object> getHash(@RequestParam String key) {
        return stringRedisTemplate.opsForHash().entries(key);
    }

    /** List：有序可重复，用 rightPushAll 从右端追加，所以第二次写的元素排在末尾 */
    @PostMapping("/list")
    public List<String> pushList(@RequestParam String key, @RequestParam List<String> values) {
        stringRedisTemplate.opsForList().rightPushAll(key, values);
        return stringRedisTemplate.opsForList().range(key, 0, -1);
    }

    /** 读 List 全部元素 */
    @GetMapping("/list")
    public List<String> getList(@RequestParam String key) {
        return stringRedisTemplate.opsForList().range(key, 0, -1);
    }

    /** Set：无序不重复，同一个值重复写不会增加元素 */
    @PostMapping("/set")
    public List<String> addSet(@RequestParam String key, @RequestParam List<String> values) {
        stringRedisTemplate.opsForSet().add(key, values.toArray(String[]::new));
        Set<String> members = stringRedisTemplate.opsForSet().members(key);
        return members == null ? List.of() : new ArrayList<>(members);
    }

    /** Set 交集，用来做"共同关注"这类查询 */
    @GetMapping("/set/intersect")
    public List<String> intersectSet(@RequestParam String key1, @RequestParam String key2) {
        Set<String> common = stringRedisTemplate.opsForSet().intersect(key1, key2);
        return common == null ? List.of() : new ArrayList<>(common);
    }

    /** ZSet：元素带分数，写入后天然按分数排序；同一个 member 再写一次是覆盖分数 */
    @PostMapping("/zset")
    public Map<String, Object> addZSet(@RequestParam String key,
                                       @RequestParam String member,
                                       @RequestParam double score) {
        stringRedisTemplate.opsForZSet().add(key, member, score);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("member", member);
        result.put("score", score);
        result.put("size", stringRedisTemplate.opsForZSet().size(key));
        return result;
    }

    /** ZSet 取分数最高的前 N 个（分数相同时按成员名排序） */
    @GetMapping("/zset/top")
    public List<Map<String, Object>> topZSet(@RequestParam String key,
                                             @RequestParam(defaultValue = "5") int top) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, top - 1L);
        if (tuples == null) {
            return List.of();
        }
        return tuples.stream()
                .map(tuple -> Map.<String, Object>of("member", tuple.getValue(), "score", tuple.getScore()))
                .toList();
    }

    /** 计数器：INCR 在 Redis 里是单线程原子的，适合统计访问量、做限流 */
    @PostMapping("/counter/incr")
    public Map<String, Object> increment(@RequestParam String key,
                                         @RequestParam(defaultValue = "1") long delta) {
        Long current = stringRedisTemplate.opsForValue().increment(key, delta);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);
        result.put("current", current);
        return result;
    }
}
