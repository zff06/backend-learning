package top.mqxu.security.controller;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * 几个 Controller 共用的取身份方法。
 */
final class CurrentUser {

    private CurrentUser() {
    }

    static String username() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "anonymous" : authentication.getName();
    }

    /**
     * 权限字符串列表。里面会包含 FACTOR_PASSWORD——那不是业务权限，
     * 是 Security 7 自动加的认证因子，展示时是否过滤由前端决定。
     */
    static List<String> authorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return List.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
