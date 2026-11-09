package top.mqxu.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * mqxu.security.* 的配置绑定。
 * <p>
 * 用可变类（@Data）而不是 record，因为测试里要改 send-interval / max-fail-count：
 * {@code properties.getSms().setSendInterval(Duration.ofMillis(50))}。
 */
@Data
@ConfigurationProperties(prefix = "mqxu.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private Sms sms = new Sms();
    private Cors cors = new Cors();

    @Data
    public static class Jwt {
        /** 密钥，64 字节时 hmacShaKeyFor 选 HS512 */
        private String secret;
        /** 有效期，yaml 里写 120 表示分钟 */
        private Duration expire = Duration.ofMinutes(120);
    }

    @Data
    public static class Sms {
        /** redis 或 memory，决定装配哪个 SmsCodeService 实现 */
        private String store = "redis";
        /** 演示开关：把验证码写在响应里。生产必须 false */
        private boolean exposeCode = false;
        private Duration codeTtl = Duration.ofMinutes(5);
        private Duration sendInterval = Duration.ofSeconds(60);
        /** 同一个验证码允许输错的次数，超过就作废 */
        private int maxFailCount = 5;
        /** 未注册手机号首次登录自动建号 */
        private boolean autoRegister = false;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
    }
}
