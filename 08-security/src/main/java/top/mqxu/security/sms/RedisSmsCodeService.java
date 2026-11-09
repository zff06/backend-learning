package top.mqxu.security.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import top.mqxu.security.config.SecurityProperties;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * 验证码存 Redis（db 11）。多实例共享，重启不丢。
 * <p>
 * 三个 key：
 * <ul>
 *   <li>sms:code:{phone}  验证码</li>
 *   <li>sms:fail:{phone}  已输错次数</li>
 *   <li>sms:limit:{phone} 发送冷却，用 setIfAbsent 实现"60 秒内只能发一次"</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mqxu.security.sms", name = "store", havingValue = "redis", matchIfMissing = true)
public class RedisSmsCodeService implements SmsCodeService {

    private static final String CODE_KEY = "sms:code:";
    private static final String FAIL_KEY = "sms:fail:";
    private static final String LIMIT_KEY = "sms:limit:";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final SecurityProperties properties;

    @Override
    public String sendCode(String phone) {
        Duration interval = properties.getSms().getSendInterval();

        // setIfAbsent 是原子的：两个并发请求只有一个能把 key 写进去
        Boolean allowed = redis.opsForValue().setIfAbsent(LIMIT_KEY + phone, "1", interval);
        if (!Boolean.TRUE.equals(allowed)) {
            throw new SmsSendTooFrequentException("验证码发送过于频繁，请稍后再试");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redis.opsForValue().set(CODE_KEY + phone, code, properties.getSms().getCodeTtl());
        redis.delete(FAIL_KEY + phone);

        log.info("短信验证码已生成 phone={} code={}（演示环境，生产不要打日志）", phone, code);
        return code;
    }

    @Override
    public boolean verify(String phone, String code) {
        String key = CODE_KEY + phone;
        String saved = redis.opsForValue().get(key);
        if (saved == null) {
            return false;
        }
        if (saved.equals(code)) {
            // 一次性：用过立刻删，同一个码不能登第二次
            redis.delete(key);
            redis.delete(FAIL_KEY + phone);
            return true;
        }

        Long fails = redis.opsForValue().increment(FAIL_KEY + phone);
        if (fails != null && fails == 1L) {
            // 首次失败时给计数 key 一个和验证码一样的过期时间，避免永久残留
            redis.expire(FAIL_KEY + phone, properties.getSms().getCodeTtl());
        }
        if (fails != null && fails >= properties.getSms().getMaxFailCount()) {
            // 错误次数用尽，验证码作废，防止暴力枚举 6 位数字
            redis.delete(key);
            redis.delete(FAIL_KEY + phone);
            log.warn("验证码错误次数超限，已作废 phone={}", phone);
        }
        return false;
    }
}
