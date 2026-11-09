package top.mqxu.security.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import top.mqxu.security.config.SecurityProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * JWT 的签发与解析。
 * <p>
 * payload 只是 Base64url 编码，不是加密——任何人拿到 token 都能解开看到内容，
 * 所以不要往里塞密码、手机号、身份证。签名解决的是防篡改，不是防偷看。
 */
@Component
public class JwtService {

    /** 权限列表在 payload 里的键名，过滤器与登录接口都要读它 */
    public static final String CLAIM_AUTHORITIES = "authorities";

    /** 带空格。"Bearer" 与 "Bearer " 是两个字符串，少一个空格就解析不出 token */
    public static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey key;
    private final Duration expire;

    public JwtService(SecurityProperties properties) {
        // hmacShaKeyFor 按字节数选算法：<32 字节 HS256，48 字节 HS384，>=64 字节 HS512。
        // 密钥太短会直接抛 WeakKeyException，不会静默降级成弱算法。
        this.key = Keys.hmacShaKeyFor(
                properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.expire = properties.getJwt().getExpire();
    }

    public String generate(String username, List<String> authorities) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_AUTHORITIES, authorities)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expire)))
                .signWith(key)
                .compact();
    }

    /**
     * 验签名、验有效期、验结构。任何一项不通过都抛 JwtException 的子类：
     * SignatureException（签名对不上）、ExpiredJwtException（过期）、MalformedJwtException（不是三段式）。
     * 调用方接住父类 JwtException 就够了。
     */
    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Authorization 头里取 token，格式不对返回 null。
     */
    public String parseAuthorizationHeader(String header) {
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length());
    }

    public long getExpiresInSeconds() {
        return expire.toSeconds();
    }
}
