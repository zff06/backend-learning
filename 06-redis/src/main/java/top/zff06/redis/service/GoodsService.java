package top.zff06.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.zff06.redis.entity.Goods;
import top.zff06.redis.mapper.GoodsMapper;

import java.util.List;

/**
 * @auther zff06
 * @data 2026/10/14
 * @description GoodsService，只查库，作为缓存效果的对照
 **/
@Service
@RequiredArgsConstructor
public class GoodsService {

    private final GoodsMapper goodsMapper;

    /** 直接查库，不碰缓存；连查两次耗时差不多，用来和缓存路径对比 */
    public Goods getFromDb(Long id) {
        return goodsMapper.selectById(id);
    }

    /** 查全部，排行榜预热与注解缓存列表都用它 */
    public List<Goods> listAll() {
        return goodsMapper.selectList(null);
    }

    /** 重置某条商品的库存，供并发扣库存实验开始前使用 */
    public int resetStock(Long id, Integer stock) {
        Goods update = new Goods();
        update.setId(id);
        update.setStock(stock);
        return goodsMapper.updateById(update);
    }
}
