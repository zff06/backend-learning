package top.zff06.security.model;

import lombok.Data;

import java.util.List;

/**
 * 认证需要的账号聚合：账号信息 + 角色编码列表
 * 实体是一张表的映射，认证要账号和角色两个信息，在这里合成
 */
@Data
public class Account {

    private Long id;

    private String username;

    private String password;

    private String nickname;

    private Boolean enabled;

    /** 角色编码，如 STUDENT / TEACHER / ADMIN */
    private List<String> roles;
}
