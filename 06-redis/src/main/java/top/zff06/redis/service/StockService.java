package top.zff06.redis.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import top.zff06.redis.config.DemoProperties;
import top.zff06.redis.entity.Goods;
import top.zff06.redis.mapper.GoodsMapper;

import java.util.concurrent.TimeUnit;

/**
 * @auther zff06
 * @data 2026/10/14
 * @description 扣库存的两种写法：不加锁（演示丢失更新）与 Redisson 分布式锁
 **/
@Service
@RequiredArgsConstructor
public class StockService {

    /** Redisson 业务锁的 key 前缀，最终形如 lock:goods:buy:1 */
    public static final String LOCK_PREFIX = "lock:goods:buy:";

    private final GoodsMapper goodsMapper;
    private final RedissonClient redissonClient;
    private final DemoProperties properties;

    /**
     * 不加锁：先读库存、再算余额、最后写回。
     * 两个请求可能读到同一个旧值，于是后写的那个把前一个的扣减盖掉，这就是丢失更新。
     * 库存扣减以数据库为准，Redis 只用来加锁，不用来存库存。
     */
    public boolean deduct(Long id, int quantity) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null || goods.getStock() < quantity) {
            return false;
        }
        Goods update = new Goods();
        update.setId(id);
        update.setStock(goods.getStock() - quantity);
        return goodsMapper.updateById(update) > 0;
    }

    /**
     * Redisson 分布式锁：满足互斥、自动释放、只释放自己的锁三条。
     * 抢不到锁的请求会在 waitSeconds 内排队，轮到就执行；leaseSeconds 到点自动释放，
     * 持有者进程崩溃也不会把锁永久留在 Redis 里。
     */
    public boolean deductWithLock(Long id, int quantity) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + id);
        try {
            // 显式传 leaseSeconds，看门狗不介入，适合锁内逻辑很短的场景
            boolean locked = lock.tryLock(properties.getLock().getWaitSeconds(),
                    properties.getLock().getLeaseSeconds(), TimeUnit.SECONDS);
            if (!locked) {
                return false;
            }
            return deduct(id, quantity);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            // 只有持锁的线程能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
