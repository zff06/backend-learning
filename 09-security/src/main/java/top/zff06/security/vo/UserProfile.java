package top.zff06.security.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 账号视图：任何响应里都不出现 password 字段
 */
@Data
@Builder
public class UserProfile {

    private String username;

    private String nickname;

    /** 角色名数组 */
    private List<String> roles;

    /** 全部权限字符串，角色名带 ROLE_ 前缀，权限点原样 */
    private List<String> authorities;

    private Boolean enabled;
}
