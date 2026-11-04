package top.zff06.security.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录响应：令牌与身份信息
 */
@Data
@Builder
public class LoginResponse {

    private String token;

    /** 固定 Bearer */
    private String tokenType;

    private LocalDateTime expiresAt;

    private String username;

    private String nickname;

    /** 角色名数组 */
    private List<String> roles;
}
