package top.mqxu.security.dto;

import java.util.List;

/**
 * 登录成功的响应体。
 * <p>
 * 注意 authorities 里的 FACTOR_PASSWORD：它不是 sec_role 里的数据，
 * 是 Spring Security 7 在认证成功时自动追加的认证因子（FactorGrantedAuthority）。
 * 前端展示权限时应该过滤掉它。
 *
 * @param token       三段式 JWT
 * @param tokenType   固定 Bearer，前端拼成 "Authorization: Bearer xxx"
 * @param expiresIn   有效期，秒
 * @param username    登录名
 * @param authorities 权限字符串列表，顺序不保证
 */
public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        String username,
        List<String> authorities) {
}
