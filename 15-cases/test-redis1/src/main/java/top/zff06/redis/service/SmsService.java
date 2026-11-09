package top.zff06.redis.service;

import top.zff06.redis.dto.LoginRequest;
import top.zff06.redis.dto.LoginResponse;

/**
 * @auther zff06
 * @data 2026/10/19
 * @description SmsService
 **/
public interface SmsService {
    /**
     * 发送验证码短信。发送失败、校验不通过都通过 BusinessException 往外抛
     *
     * @param phone 手机号
     */
    void sendSms(String phone);

    /**
     * 验证码登录
     *
     * @param loginRequest 登录请求参数
     * @return 登录响应
     */
    LoginResponse login(LoginRequest loginRequest);
}
