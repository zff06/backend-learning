package top.zff06.redis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.*;
import top.zff06.redis.entity.Goods;
import top.zff06.redis.service.GoodsService;
import top.zff06.redis.service.StockService;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @auther zff06
 * @data 2026/10/14
 * @description 并发扣库存的三种写法对照：不加锁 / Redisson / 手写 SETNX
 **/
@RestController
@RequestMapping("/lock")
@RequiredArgsConstructor
public class LockController {

    /** 手写 SETNX 锁的 key 前缀 */
    private static final String SETNX_LOCK_PREFIX = "lock:goods:setnx:";

    /** 手写锁的过期时间，写死；持锁时间长于它会提前丢锁，这正是 Redisson 看门狗要解决的问题 */
    private static final Duration SETNX_LOCK_TTL = Duration.ofSeconds(10);

    /**
     * 释放锁时先比对持有者标记，值对得上才删，避免误删别人持有的锁。
     * 比对与删除写在同一个 Lua 脚本里，Redis 执行脚本期间不会插入其他命令，所以这两步是原子的；
     * 拆成先 get 再 del 两条命令的话，中间可能正好锁过期、别人拿到新锁，删掉的就是别人的锁。
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final GoodsService goodsService;
    private final StockService stockService;

    /** 每次实验前重置库存，否则上一轮的结果会带进下一轮 */
    @PostMapping("/reset")
    public Map<String, Object> reset(@RequestParam Long id, @RequestParam Integer stock) {
        goodsService.resetStock(id, stock);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("stock", stock);
        return result;
    }

    /** 查库存，绕过所有缓存直接查库，看到的必然是数据库里的真实值 */
    @GetMapping("/stock/{id}")
    public Goods stock(@PathVariable Long id) {
        return goodsService.getFromDb(id);
    }

    /** 不加锁购买：并发下会丢失更新，卖出的比扣掉的多 */
    @PostMapping("/buy/no-lock")
    public Map<String, Object> buyWithoutLock(@RequestParam Long id,
                                              @RequestParam(defaultValue = "1") int quantity) {
        return result(id, stockService.deduct(id, quantity), "no-lock");
    }

    /** Redisson 购买：没抢到锁的请求会在等待时间内排队 */
    @PostMapping("/buy/redisson")
    public Map<String, Object> buyWithRedisson(@RequestParam Long id,
                                               @RequestParam(defaultValue = "1") int quantity) {
        return result(id, stockService.deductWithLock(id, quantity), "redisson");
    }

    /** 手写 SETNX 购买：抢不到锁直接返回，不做等待 */
    @PostMapping("/buy/setnx")
    public Map<String, Object> buyWithSetNx(@RequestParam Long id,
                                            @RequestParam(defaultValue = "1") int quantity) {
        String lockKey = SETNX_LOCK_PREFIX + id;
        String token = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, token, SETNX_LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            Map<String, Object> refused = new LinkedHashMap<>();
            refused.put("strategy", "setnx");
            refused.put("success", false);
            refused.put("reason", "没抢到锁，已有请求在处理");
            return refused;
        }
        try {
            return result(id, stockService.deduct(id, quantity), "setnx");
        } finally {
            stringRedisTemplate.execute(UNLOCK_SCRIPT, List.of(lockKey), token);
        }
    }

    /** 查某个 key 的剩余过期时间，用来确认锁有没有真的释放 */
    @GetMapping("/info")
    public Map<String, Object> lockInfo(@RequestParam String key) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key);
        result.put("ttlSeconds", stringRedisTemplate.getExpire(key));
        return result;
    }

    /** 三种写法共用的返回体，stockLeft 是操作完之后数据库里的真实库存 */
    private Map<String, Object> result(Long id, boolean success, String strategy) {
        Goods goods = goodsService.getFromDb(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("strategy", strategy);
        result.put("success", success);
        result.put("stockLeft", goods == null ? null : goods.getStock());
        return result;
    }
}
