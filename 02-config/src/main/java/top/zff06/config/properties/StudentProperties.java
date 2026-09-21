package top.zff06.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @auther zff06
 * @data 2026/9/16
 * @description StudentProperties
 **/
@Data
@Component
@ConfigurationProperties(prefix = "student")
public class StudentProperties {
    private String name;
    private Integer age;
    private List<String> hobbies;
    private Map<String,Integer> scores;
    private Address address;
    private List<Course> courses;

    @Data
    public static class  Address{
        private String province;
        private String city;
    }

    @Data
    public static class Course{
        private String name;
        private Integer credit;
    }
}
