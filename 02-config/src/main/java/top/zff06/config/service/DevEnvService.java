package top.zff06.config.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * @auther zff06
 * @data 2026/9/16
 * @description DevEnvService
 **/
@Service
@Profile("dev")
public class DevEnvService implements EnvService {

    @Override
    public String envInfo(){
        return "我是 dev 环境专属的Bean";
    }
}
