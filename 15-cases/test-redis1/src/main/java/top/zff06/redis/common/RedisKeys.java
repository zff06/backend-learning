package top.zff06.redis.common;

/**
 * @auther zff06
 * @data 2026/10/19
 * @description RedisKeys
 **/
public class RedisKeys {
    public static String getSmsKey(String phone) {
        return "sms:captcha:" + phone;
    }

    /**
     * 发送间隔 Key，存在即代表还在冷却中
     */
    public static String getSmsSendIntervalKey(String phone) {
        return "sms:send:interval:" + phone;
    }

    /**
     * 当日发送次数 Key
     */
    public static String getSmsSendCountKey(String phone) {
        return "sms:send:count:" + phone;
    }

    /**
     * 验证码连续输错次数 Key
     */
    public static String getSmsFailCountKey(String phone) {
        return "sms:fail:" + phone;
    }
}
