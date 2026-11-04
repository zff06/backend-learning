package top.zff06.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置，密钥与有效期都从配置文件读，不写死在代码里
 */
@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** HS256 签名密钥，生产环境用环境变量 JWT_SECRET 覆盖 */
    private String secret;

    /** 签发方标识 */
    private String issuer;

    /** 有效期（分钟） */
    private int expireMinutes;
}
