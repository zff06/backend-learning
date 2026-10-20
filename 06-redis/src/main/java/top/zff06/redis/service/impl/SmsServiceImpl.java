package top.zff06.redis.service.impl;

import com.cloopen.rest.sdk.BodyType;
import com.cloopen.rest.sdk.CCPRestSmsSDK;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import top.zff06.redis.config.CloopenConfig;
import top.zff06.redis.service.SmsService;

import java.time.Duration;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @auther zff06
 * @data 2026/10/14
 * @description SmsServiceImpl
 **/
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsServiceImpl implements SmsService {

    /** 验证码在 Redis 里的 key 前缀，后面拼手机号 */
    private static final String CODE_KEY_PREFIX = "sms:code:";

    /** 验证码有效期：5 分钟 */
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    private final CloopenConfig cloopenConfig;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean sendSms(String phone) {
        // 生成随机四位验证码：nextInt 的上界是开区间，1000~9999 刚好保证首位不为 0
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 10000));

        CCPRestSmsSDK sdk = new CCPRestSmsSDK();
        sdk.init(cloopenConfig.getServerIp(), cloopenConfig.getPort());
        sdk.setAccount(cloopenConfig.getAccountSID(), cloopenConfig.getAccountToken());
        sdk.setAppId(cloopenConfig.getAppId());
        sdk.setBodyType(BodyType.Type_JSON);
        String templateId = cloopenConfig.getTemplateId();
        // 短信模板里的占位符是 {1}，这个数组按顺序填进去
        String[] datas = {code};
        HashMap<String, Object> result = sdk.sendTemplateSMS(phone, templateId, datas);
        if ("000000".equals(result.get("statusCode"))) {
            HashMap<String, Object> data = (HashMap<String, Object>) result.get("data");
            Set<String> keySet = data.keySet();
            for (String key : keySet) {
                Object object = data.get(key);
                log.info("key = {}", object);
            }
            // 发送成功才写 Redis，避免发不出去还留下一个能校验通过的码
            stringRedisTemplate.opsForValue().set(CODE_KEY_PREFIX + phone, code, CODE_TTL);
            log.info("验证码已发送，手机号 = {}，验证码 = {}（开发环境打印便于本地调试）", phone, code);
            return true;
        } else {
            log.info("错误码: {},错误信息:{}", result.get("statusCode"), result.get("statusMsg"));
            return false;
        }
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String key = CODE_KEY_PREFIX + phone;
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null && cached.equals(code)) {
            // 校验通过就删掉，保证一次一码；缓存本身也会在 TTL 到期后自动消失
            stringRedisTemplate.delete(key);
            return true;
        }
        return false;
    }
}
