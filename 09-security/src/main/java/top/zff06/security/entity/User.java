package top.zff06.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账号实体，对应 sec_user 表
 * 实体只做表结构映射，不实现 Spring Security 的 UserDetails，两边解耦
 */
@Data
@TableName("sec_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    /** 1 启用 / 0 停用 */
    private Integer enabled;

    private LocalDateTime createdAt;
}
