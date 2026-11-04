package top.zff06.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @auther zff06
 * @data 2026/11/4
 * @description OssConfig
 **/
@Data
@ConfigurationProperties(prefix="aliyun-oss")
@PropertySource("classpath:aliyun-oss.properties")
@Configuration
public class OssConfig {
    private String endpoint;
    private String bucket;
    private String dir;
    private String accessKey;
    private String secretKey;
}
