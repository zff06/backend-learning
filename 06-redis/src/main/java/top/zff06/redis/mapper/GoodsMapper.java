package top.zff06.redis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.zff06.redis.entity.Goods;

/**
 * @auther zff06
 * @data 2026/10/14
 * @description GoodsMapper，缓存三组接口共同的数据库来源
 **/
@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

}
