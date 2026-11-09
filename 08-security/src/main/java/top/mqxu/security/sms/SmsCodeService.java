package top.mqxu.security.sms;

/**
 * 验证码的存取接口。
 * <p>
 * Redis 与内存两个实现都实现它，用 @ConditionalOnProperty 二选一装配，
 * SmsCodeAuthenticationProvider 完全不知道底层是哪个——换存储不用改 Provider。
 */
public interface SmsCodeService {

    /**
     * 生成并保存一个 6 位验证码，返回明文（演示模式下要回显给前端）。
     * 距上次发送不足 send-interval 时抛 {@link SmsSendTooFrequentException}。
     */
    String sendCode(String phone);

    /**
     * 校验验证码。校验成功立刻删除（一次性）；失败累计次数，超过 max-fail-count 作废。
     *
     * @return true 通过
     */
    boolean verify(String phone, String code);
}
