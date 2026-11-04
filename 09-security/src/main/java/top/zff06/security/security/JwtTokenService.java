package top.zff06.security.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import top.zff06.security.config.JwtProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 令牌签发与解析
 * 载荷只放身份（用户名、昵称、角色名），不放权限点：
 * 权限每次请求回库查，管理员改完角色旧令牌立刻生效
 */
@Component
public class JwtTokenService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** 签发令牌，subject 是用户名 */
    public String generateToken(String username, String nickname) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + properties.getExpireMinutes() * 60_000L);
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(username)
                .claim("nickname", nickname)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(key)
                .compact();
    }

    /**
     * 验签并解析，拿到用户名
     * 令牌被改过或过期会抛 JwtException，由过滤器兜住转成 401
     */
    public String parseUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /** 过期时间，给登录响应里的 expiresAt 用 */
    public LocalDateTime expiresAt() {
        return LocalDateTime.now().plusMinutes(properties.getExpireMinutes());
    }
}
