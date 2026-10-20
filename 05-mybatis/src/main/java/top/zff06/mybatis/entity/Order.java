package top.zff06.mybatis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @auther zff06
 * @data 2026/10/11
 * @description Order 订单实体：createTime / updateTime / deleted 三个审计字段复用全局配置
 **/
@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号，数据库上有唯一索引 uk_order_no */
    private String orderNo;

    private Long courseId;

    private Integer quantity;

    private BigDecimal amount;

    /** 状态，1 待支付 */
    private Integer status;

    /** 插入时由 MyMetaObjectHandler 自动填充，不需要手动 set */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 逻辑删除：delete 变成 update deleted = 1，查询自动追加 deleted = 0 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
