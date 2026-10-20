package top.zff06.redis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @auther zff06
 * @data 2026/10/14
 * @description 商品实体，对应 t_goods
 **/
@Data
@TableName("t_goods")
public class Goods {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品名称 */
    private String name;

    /** 分类：数码 / 家居 / 图书 / 运动 / 食品 */
    private String category;

    /** 价格，金额用定点数 */
    private BigDecimal price;

    /** 库存，扣减以数据库为准，Redis 只用来加锁 */
    private Integer stock;

    /** 销量，排行榜里当 ZSet 的分数 */
    private Integer sales;

    /** 状态：1 上架，0 下架 */
    private Integer status;

    /** 创建时间，由表上的 DEFAULT CURRENT_TIMESTAMP 填充 */
    private LocalDateTime createTime;

    /** 更新时间，由表上的 ON UPDATE CURRENT_TIMESTAMP 填充 */
    private LocalDateTime updateTime;
}
