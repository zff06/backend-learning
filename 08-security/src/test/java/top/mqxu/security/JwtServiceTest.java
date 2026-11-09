package top.mqxu.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.mqxu.security.config.SecurityProperties;
import top.mqxu.security.security.JwtService;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtService 的四个用例：签发解析、篡改、过期、换密钥。
 * <p>
 * 纯单元测试，不起 Spring 容器——JwtService 只依赖一个配置对象。
 */
class JwtServiceTest {

    private static final String SECRET_64 =
            "3f2b8d1e7c9a4f6b0d5e8a2c7f1b9d4e6a3c8f2b5d7e9a1c4f6b8d2e5a7c9f1b";
    private static final String SECRET_OTHER =
            "9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b";

    private static JwtService service(String secret, Duration expire) {
        SecurityProperties properties = new SecurityProperties();
        properties.getJwt().setSecret(secret);
        properties.getJwt().setExpire(expire);
        return new JwtService(properties);
    }

    @Test
    @DisplayName("签发后可解析出用户名与权限列表")
    void generateThenParse() {
        JwtService jwtService = service(SECRET_64, Duration.ofMinutes(120));

        String token = jwtService.generate("alice", List.of("ROLE_USER", "ARTICLE_DELETE"));

        // 三段式：header.payload.signature
        assertThat(token.split("\\.")).hasSize(3);

        Claims claims = jwtService.parse(token);
        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(claims.get(JwtService.CLAIM_AUTHORITIES, List.class))
                .asList()
                .containsExactlyInAnyOrder("ROLE_USER", "ARTICLE_DELETE");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    @DisplayName("改签名段一个字符，验签必须失败")
    void tamperedSignatureIsRejected() {
        JwtService jwtService = service(SECRET_64, Duration.ofMinutes(120));
        String token = jwtService.generate("alice", List.of("ROLE_USER"));

        // 坑：不要改最后一个字符。HS512 签名是 64 字节，Base64url 编码成 86 个字符，
        // 最后一个字符只有 2 位有效、4 位是填充，改它可能只动到填充位，解码后的字节不变。
        // 所以要改签名段的第一个字符。
        int signatureStart = token.lastIndexOf('.') + 1;
        char original = token.charAt(signatureStart);
        String tampered = token.substring(0, signatureStart)
                + (original == 'A' ? 'B' : 'A')
                + token.substring(signatureStart + 1);

        assertThatThrownBy(() -> jwtService.parse(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("过期 token 抛 ExpiredJwtException")
    void expiredTokenIsRejected() {
        // 负数有效期：签发的一瞬间就过期了，不用 sleep
        JwtService jwtService = service(SECRET_64, Duration.ofSeconds(-10));
        String token = jwtService.generate("alice", List.of("ROLE_USER"));

        assertThatThrownBy(() -> jwtService.parse(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("换一把密钥，旧 token 验签失败")
    void tokenFromAnotherKeyIsRejected() {
        String token = service(SECRET_64, Duration.ofMinutes(120))
                .generate("alice", List.of("ROLE_USER"));

        JwtService other = service(SECRET_OTHER, Duration.ofMinutes(120));

        assertThatThrownBy(() -> other.parse(token))
                .isInstanceOf(JwtException.class);
    }
}
