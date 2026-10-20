package top.zff06.redis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.*;
import top.zff06.redis.entity.Goods;
import top.zff06.redis.service.GoodsService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @auther zff06
 * @data 2026/10/14
 * @description ZSet 排行榜：商品 id 当成员、销量当分数，排序做在写入时
 **/
@RestController
@RequestMapping("/rank")
@RequiredArgsConstructor
public class RankController {

    /** 销量排行榜的 ZSet key */
    private static final String SALES_RANK_KEY = "rank:goods:sales";

    private final StringRedisTemplate stringRedisTemplate;
    private final GoodsService goodsService;

    /** 把数据库里的销量灌进 ZSet，做一次缓存预热；同一个成员是覆盖分数，重复预热不会重复计数 */
    @PostMapping("/warmup")
    public Map<String, Object> warmUp() {
        List<Goods> goodsList = goodsService.listAll();
        for (Goods goods : goodsList) {
            stringRedisTemplate.opsForZSet()
                    .add(SALES_RANK_KEY, String.valueOf(goods.getId()), goods.getSales());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("warmed", goodsList.size());
        result.put("size", stringRedisTemplate.opsForZSet().size(SALES_RANK_KEY));
        return result;
    }

    /** 给某个商品加分，ZSet 会自动把它挪到正确位置；incrementScore 是原子的，并发加分不会互相覆盖 */
    @PostMapping("/increase")
    public Map<String, Object> increase(@RequestParam Long id,
                                        @RequestParam(defaultValue = "1") double delta) {
        Double score = stringRedisTemplate.opsForZSet().incrementScore(SALES_RANK_KEY, String.valueOf(id), delta);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("goodsId", id);
        result.put("score", score);
        result.put("rank", rankOf(id));
        return result;
    }

    /** 取销量前 N 名，name 是拿 ZSet 里的 goodsId 回查数据库补上的 */
    @GetMapping("/top")
    public List<Map<String, Object>> top(@RequestParam(defaultValue = "5") int top) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(SALES_RANK_KEY, 0, top - 1L);
        if (tuples == null) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String goodsId = tuple.getValue();
            Goods goods = goodsId == null ? null : goodsService.getFromDb(Long.valueOf(goodsId));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("goodsId", goodsId);
            item.put("sales", tuple.getScore());
            item.put("name", goods == null ? null : goods.getName());
            result.add(item);
        }
        return result;
    }

    /** 查某个商品的名次，名次从 1 开始；没上过榜返回 rank=-1、sales=null */
    @GetMapping("/rank-of")
    public Map<String, Object> rankOfApi(@RequestParam Long id) {
        String member = String.valueOf(id);
        Integer rank = rankOf(id);
        Double score = stringRedisTemplate.opsForZSet().score(SALES_RANK_KEY, member);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("goodsId", id);
        result.put("rank", rank);
        result.put("sales", score);
        return result;
    }

    /** reverseRank 的返回值从 0 开始，所以加 1；不在榜上返回 null，接口转成 -1 */
    private Integer rankOf(Long id) {
        Long rank = stringRedisTemplate.opsForZSet().reverseRank(SALES_RANK_KEY, String.valueOf(id));
        return rank == null ? -1 : (int) (rank + 1);
    }
}
