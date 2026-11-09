package top.mqxu.security.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import top.mqxu.security.config.SecurityProperties;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码存进程内。把 mqxu.security.sms.store 改成 memory 就切到这个实现，不连 Redis 也能跑通链路。
 * <p>
 * 代价：重启即失效、多实例不共享。只适合本地跑通，不能上生产。
 * <p>
 * 用 ConcurrentHashMap 而不是 Caffeine，是因为这里要测"发送间隔"和"过期"，
 * 自己控时间戳比引入一个缓存库更好解释。
 */
@Service
@ConditionalOnProperty(prefix = "mqxu.security.sms", name = "store", havingValue = "memory")
public class InMemorySmsCodeService implements SmsCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** phone -> 验证码条目 */
    private final Map<String, Entry> codes = new ConcurrentHashMap<>();
    /** phone -> 下次允许发送的时间点 */
    private final Map<String, Instant> limits = new ConcurrentHashMap<>();

    private final SecurityProperties properties;

    public InMemorySmsCodeService(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public String sendCode(String phone) {
        Instant now = Instant.now();
        Instant allowedAt = limits.get(phone);
        if (allowedAt != null && now.isBefore(allowedAt)) {
            throw new SmsSendTooFrequentException("验证码发送过于频繁，请稍后再试");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        codes.put(phone, new Entry(code, now.plus(properties.getSms().getCodeTtl())));
        limits.put(phone, now.plus(properties.getSms().getSendInterval()));
        return code;
    }

    @Override
    public boolean verify(String phone, String code) {
        Entry entry = codes.get(phone);
        if (entry == null) {
            return false;
        }
        if (Instant.now().isAfter(entry.expireAt())) {
            codes.remove(phone);
            return false;
        }
        if (entry.code().equals(code)) {
            codes.remove(phone);
            return true;
        }

        int fails = entry.failCount() + 1;
        if (fails >= properties.getSms().getMaxFailCount()) {
            codes.remove(phone);
        } else {
            codes.put(phone, entry.withFailCount(fails));
        }
        return false;
    }

    /**
     * 仅测试用：把发送冷却清掉，方便同一个手机号连发。
     */
    public void clearLimit(String phone) {
        limits.remove(phone);
    }

    /**
     * 仅测试用：把冷却时间改成毫秒级，否则测一次等一分钟。
     */
    public void resetSendInterval(Duration interval) {
        properties.getSms().setSendInterval(interval);
    }

    private record Entry(String code, Instant expireAt, int failCount) {

        Entry(String code, Instant expireAt) {
            this(code, expireAt, 0);
        }

        Entry withFailCount(int fails) {
            return new Entry(code, expireAt, fails);
        }
    }
}
