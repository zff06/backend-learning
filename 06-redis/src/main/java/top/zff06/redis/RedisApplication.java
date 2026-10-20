package top.zff06.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import top.zff06.redis.config.DemoProperties;

/**
 * @auther zff06
 * @data 2026/9/23
 * @description RedisApplication
 **/
@SpringBootApplication
// 打开缓存注解：没有它 @Cacheable / @CachePut / @CacheEvict 不会生效，而且不会报错
@EnableCaching
// 把 application.yml 里 demo.* 那一组业务参数绑定成 DemoProperties 对象
@EnableConfigurationProperties(DemoProperties.class)
public class RedisApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedisApplication.class,args);
    }
}
