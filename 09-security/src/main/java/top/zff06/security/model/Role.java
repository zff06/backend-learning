package top.zff06.security.model;

import lombok.Getter;

import java.util.List;

/**
 * 角色枚举：权限点跟着角色走，留在这里而不是建表
 * 角色是运行期数据存库里，权限点是接口约定留代码里，好评审
 */
@Getter
public enum Role {

    STUDENT(List.of("task:submit", "task:read")),
    TEACHER(List.of("task:review", "task:read")),
    ADMIN(List.of("user:read", "user:write", "task:read", "task:review"));

    private final List<String> authorities;

    Role(List<String> authorities) {
        this.authorities = authorities;
    }

    /** 按角色编码查枚举，库里出现未知编码时返回 null */
    public static Role fromCode(String code) {
        for (Role role : values()) {
            if (role.name().equals(code)) {
                return role;
            }
        }
        return null;
    }
}
