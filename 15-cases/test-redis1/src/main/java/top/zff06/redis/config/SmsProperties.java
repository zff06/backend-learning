package top.zff06.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @auther zff06
 * @data 2026/10/19
 * @description SmsProperties
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "zff06.sms")
public class SmsProperties {
    private boolean mockEnabled = true;

    /**
     * 验证码有效期，单位秒
     */
    private long codeExpireSeconds = 300;

    /**
     * 同一个手机号两次发送之间的最小间隔，单位秒
     */
    private long sendIntervalSeconds = 60;

    /**
     * 同一个手机号一天最多发送的条数
     */
    private long sendDailyLimit = 10;

    /**
     * 同一个验证码最多允许输错的次数，超过就作废
     */
    private int maxFailCount = 5;
}
