package top.zff06.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @auther zff06
 * @data 2026/10/11
 * @description Course
 **/
@Data
@TableName("t_course")
public class Course {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String teacher;
    private Integer credit;
    /** 库存，练习5 下单时用 ge(stock, quantity) 做乐观条件扣减 */
    private Integer stock;

}
