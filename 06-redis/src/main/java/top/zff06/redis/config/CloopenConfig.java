package top.zff06.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 容联云短信配置，对应 application.yml 中 zff06.sms.ccp 前缀
 *
 * @auther zff06
 * @data 2026/10/14
 * @description CloopenConfig
 **/
@Data
@Component
@ConfigurationProperties(prefix = "zff06.sms.ccp")
public class CloopenConfig {
    /** 服务器地址 */
    private String serverIp;
    /** 端口 */
    private String port;
    /** 主账户 ID */
    private String accountSID;
    /** 主账户令牌 */
    private String accountToken;
    /** 应用 ID */
    private String appId;
    /** 短信模板 ID */
    private String templateId;
}
