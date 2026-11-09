package top.zff06.redis.controller;

import io.jsonwebtoken.Claims;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.redis.common.Result;
import top.zff06.redis.config.AuthInterceptor;
import top.zff06.redis.dto.UserInfo;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @auther zff06
 * @data 2026/10/19
 * @description UserController
 **/
@RestController
@RequestMapping("/user")
public class UserController {
    private static final DateTimeFormatter LOGIN_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 返回当前 token 对应的用户信息，前端首页用它替代读 localStorage
     */
    @GetMapping("/me")
    public Result<UserInfo> me(@RequestAttribute(AuthInterceptor.ATTR_CLAIMS) Claims claims) {
        String loginTime = LOGIN_TIME_FORMATTER.format(
                claims.getIssuedAt().toInstant().atZone(ZoneId.systemDefault()));
        return Result.ok(new UserInfo(claims.getSubject(), loginTime));
    }
}
