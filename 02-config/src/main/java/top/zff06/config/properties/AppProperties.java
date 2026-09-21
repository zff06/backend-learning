package top.zff06.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * @auther zff06
 * @data 2026/9/16
 * @description AppProperties
 **/
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    @NotBlank(message = "应用名称 app.name 不能为空")
    private String name;

    private String author;
    @Min(value = 1,message = "app.port 必须大于等于1")
    @Max(value = 65535,message = "app.port 必须小于等于 65535")
    private Integer port;

    @Min(value = 1,message = "app.max-count 必须大于等于1")
    @Max(value = 1000,message = "app.max-count 不能超过1000")
    private Integer maxCount;
}
