package top.mqxu.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.mqxu.security.config.SecurityProperties;
import top.mqxu.security.sms.InMemorySmsCodeService;
import top.mqxu.security.sms.SmsSendTooFrequentException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 内存实现的五个用例：一次性、错误次数、发送间隔、过期、前导零。
 * <p>
 * 每个用例 @BeforeEach 新建一个 service，不用清任何缓存。
 * 发送间隔与有效期都调到毫秒级，否则测一次要等一分钟。
 */
class InMemorySmsCodeTest {

    private static final String PHONE = "13900001111";

    private SecurityProperties properties;
    private InMemorySmsCodeService service;

    @BeforeEach
    void setUp() {
        properties = new SecurityProperties();
        properties.getSms().setSendInterval(Duration.ofMillis(20));
        properties.getSms().setCodeTtl(Duration.ofMillis(300));
        properties.getSms().setMaxFailCount(3);
        service = new InMemorySmsCodeService(properties);
    }

    @Test
    @DisplayName("验证码只能用一次，用过即失效")
    void codeIsOneTime() {
        String code = service.sendCode(PHONE);

        assertThat(service.verify(PHONE, code)).isTrue();
        assertThat(service.verify(PHONE, code)).isFalse();
    }

    @Test
    @DisplayName("连续输错达到上限后，正确的验证码也作废")
    void wrongCodeTimesOut() {
        String code = service.sendCode(PHONE);

        // maxFailCount = 3，前两次只是记数，第三次到上限就删掉
        assertThat(service.verify(PHONE, "000000")).isFalse();
        assertThat(service.verify(PHONE, "000000")).isFalse();
        assertThat(service.verify(PHONE, "000000")).isFalse();

        assertThat(service.verify(PHONE, code)).isFalse();
    }

    @Test
    @DisplayName("冷却期内重复发送被拒绝")
    void sendIntervalIsEnforced() {
        service.sendCode(PHONE);

        assertThatThrownBy(() -> service.sendCode(PHONE))
                .isInstanceOf(SmsSendTooFrequentException.class);
    }

    @Test
    @DisplayName("过期后验证码不可用")
    void expiredCodeIsRejected() throws InterruptedException {
        String code = service.sendCode(PHONE);

        Thread.sleep(400);

        assertThat(service.verify(PHONE, code)).isFalse();
    }

    @Test
    @DisplayName("验证码固定六位，带前导零时按字符串比对")
    void codesKeepLeadingZeros() {
        // 生成时用 %06d 补零，如果中间转成 int 再比，013579 会变成 13579，永远对不上。
        // 这里连续发若干个码，断言它们都是六位字符串。
        for (int i = 0; i < 20; i++) {
            service.clearLimit(PHONE);
            String code = service.sendCode(PHONE);
            assertThat(code).hasSize(6).matches("\\d{6}");
            // 字符串比对能通过（包括带前导零的情况）
            assertThat(service.verify(PHONE, code)).isTrue();
        }
    }
}
