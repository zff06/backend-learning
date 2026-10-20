package top.zff06.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import top.zff06.redis.entity.Goods;
import top.zff06.redis.mapper.GoodsMapper;

import java.math.BigDecimal;
import java.util.List;

/**
 * @auther zff06
 * @data 2026/10/14
 * @description 用 Spring 缓存注解代替手写缓存，读写逻辑从业务方法里抽走
 * <p>
 * 注意两点：类必须是容器里的 Bean；方法要从外部调用，同一个类内部自调用会绕过代理，注解不生效。
 **/
@Service
@RequiredArgsConstructor
public class GoodsSpringCacheService {

    private final GoodsMapper goodsMapper;

    /**
     * 命中缓存时方法体不会执行。
     * 查不到数据会返回 null，配合 spring.cache.redis.cache-null-values=true 把 null 也存进缓存，
     * 不需要额外写空值标记就能起到防穿透的作用。
     */
    @Cacheable(cacheNames = "goods", key = "#id")
    public Goods getById(Long id) {
        return goodsMapper.selectById(id);
    }

    /** 查列表，结果放在 goods 区域下固定的 all 键上 */
    @Cacheable(cacheNames = "goods", key = "'all'")
    public List<Goods> listAll() {
        return goodsMapper.selectList(null);
    }

    /** 更新数据库，并把新值写回缓存（与手动缓存的"更新库后删缓存"形成对照） */
    @CachePut(cacheNames = "goods", key = "#id")
    public Goods updatePrice(Long id, BigDecimal price) {
        Goods update = new Goods();
        update.setId(id);
        update.setPrice(price);
        if (goodsMapper.updateById(update) == 0) {
            return null;
        }
        return goodsMapper.selectById(id);
    }

    /** 同时清掉单个商品与列表两处缓存：只清单个 key 的话，列表接口返回的还是旧值 */
    @Caching(evict = {
            @CacheEvict(cacheNames = "goods", key = "#id"),
            @CacheEvict(cacheNames = "goods", key = "'all'")
    })
    public void evictDetailAndList(Long id) {
        // 改动了商品数据，明细与列表都要失效
    }

    /** 清空整个 goods 缓存区域 */
    @CacheEvict(cacheNames = "goods", allEntries = true)
    public void evictAll() {
        // 只负责清缓存
    }
}
