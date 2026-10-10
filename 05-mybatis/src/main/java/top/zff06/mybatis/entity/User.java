package top.zff06.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @auther zff06
 * @data 2026/10/10
 * @description User
 **/
@Data
@TableName("t_user1")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private Integer age;
    private LocalDateTime createTime;
}
