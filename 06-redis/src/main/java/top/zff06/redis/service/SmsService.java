package top.zff06.redis.service;

/**
 * @auther zff06
 * @data 2026/10/14
 * @description SmsService
 **/
public interface SmsService {

    /**
     * 生成随机四位验证码，发送到指定手机号，并把验证码写进 Redis（带过期时间）
     *
     * @param phone 手机号
     * @return 是否发送成功
     */
    boolean sendSms(String phone);

    /**
     * 校验手机号对应的验证码，校验通过后立即删除，防止同一个码被重复使用
     *
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @return 是否校验通过
     */
    boolean verifyCode(String phone, String code);
}
