package top.mqxu.security.dto;

import java.util.List;

/**
 * 当前登录用户的信息，/api/me 与 /stage4/me 共用。
 *
 * @param username    登录名
 * @param authorities 权限字符串列表
 */
public record UserInfo(String username, List<String> authorities) {
}
