package top.zff06.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @auther zff06
 * @data 2026/10/19
 * @description CloopenConfig
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "zff06.sms.ccp")
public class CloopenConfig {
    private String serverIp;
    private String port;
    private String accountSID;
    private String accountToken;
    private String appId;
    private String templateId;
}
