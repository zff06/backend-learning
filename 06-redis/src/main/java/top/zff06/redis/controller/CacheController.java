package top.zff06.redis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.zff06.redis.dto.CacheResult;
import top.zff06.redis.entity.Goods;
import top.zff06.redis.service.GoodsCacheService;
import top.zff06.redis.service.GoodsService;
import top.zff06.redis.service.GoodsSpringCacheService;

import java.math.BigDecimal;
import java.util.List;

/**
 * @auther zff06
 * @data 2026/10/14
 * @description 三种缓存做法的对照：直查库 / 手动缓存 / 注解缓存
 * <p>
 * 返回体里的 source 说明这次数据从哪来，配合耗时一眼就能看出缓存有没有生效。
 **/
@RestController
@RequestMapping("/cache/goods")
@RequiredArgsConstructor
public class CacheController {

    /** 注解缓存拿不到"这次有没有命中"的标记，只能看耗时和 SQL 日志 */
    private static final String SOURCE_ANNO = "anno（是否命中看日志）";

    private final GoodsService goodsService;
    private final GoodsCacheService cacheService;
    private final GoodsSpringCacheService springCacheService;

    /** 直查库，不碰缓存，给后面的接口留一个耗时对照 */
    @GetMapping("/db/{id}")
    public CacheResult<Goods> fromDb(@PathVariable Long id) {
        long start = System.nanoTime();
        Goods goods = goodsService.getFromDb(id);
        return new CacheResult<>(goods, "db", CacheResult.costFrom(start));
    }

    /** 手动缓存：先查缓存，未命中查库并回填；查不到会写空值标记 */
    @GetMapping("/manual/{id}")
    public CacheResult<Goods> manual(@PathVariable Long id) {
        return cacheService.getById(id);
    }

    /** 手动缓存 + 互斥锁：并发重建时只放一个请求回源 */
    @GetMapping("/manual-mutex/{id}")
    public CacheResult<Goods> manualWithMutex(@PathVariable Long id) {
        return cacheService.getByIdWithMutex(id);
    }

    /** 注解缓存单个商品 */
    @GetMapping("/anno/{id}")
    public CacheResult<Goods> anno(@PathVariable Long id) {
        long start = System.nanoTime();
        Goods goods = springCacheService.getById(id);
        return new CacheResult<>(goods, SOURCE_ANNO, CacheResult.costFrom(start));
    }

    /** 注解缓存列表，key 是固定的 all */
    @GetMapping("/anno/list")
    public CacheResult<List<Goods>> annoList() {
        long start = System.nanoTime();
        List<Goods> list = springCacheService.listAll();
        return new CacheResult<>(list, SOURCE_ANNO, CacheResult.costFrom(start));
    }

    /** 改价：先更新库、再删缓存，然后重新读一次，所以 source 又是 db */
    @PutMapping("/{id}/price")
    public CacheResult<Goods> updatePrice(@PathVariable Long id, @RequestParam BigDecimal price) {
        long start = System.nanoTime();
        cacheService.updatePrice(id, price);
        Goods goods = cacheService.getById(id).data();
        return new CacheResult<>(goods, "db", CacheResult.costFrom(start));
    }

    /** 改价：注解版用 @CachePut，不删旧值而是把新值写回缓存 */
    @PutMapping("/{id}/price-anno")
    public CacheResult<Goods> updatePriceAnno(@PathVariable Long id, @RequestParam BigDecimal price) {
        long start = System.nanoTime();
        Goods goods = springCacheService.updatePrice(id, price);
        return new CacheResult<>(goods, "anno-put", CacheResult.costFrom(start));
    }

    /** 只删手动缓存的单个 key */
    @DeleteMapping("/manual/{id}/cache")
    public boolean evictManual(@PathVariable Long id) {
        return cacheService.evict(id);
    }

    /** 清注解缓存的明细与列表：列表里也含这个商品，只清明细会让列表返回旧值 */
    @DeleteMapping("/anno/{id}")
    public String evictAnno(@PathVariable Long id) {
        springCacheService.evictDetailAndList(id);
        return "已清掉 goods 区域下 id=" + id + " 与 all 两处缓存";
    }

    /** 清空整个 goods 缓存区域 */
    @DeleteMapping("/anno")
    public String evictAnnoAll() {
        springCacheService.evictAll();
        return "已清空 goods 缓存区域";
    }
}
