package top.zff06.redis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.zff06.redis.common.Result;
import top.zff06.redis.dto.LoginRequest;
import top.zff06.redis.dto.LoginResponse;
import top.zff06.redis.service.SmsService;

/**
 * @auther zff06
 * @data 2026/10/19
 * @description SmsController
 **/
@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
public class SmsController {
    private final SmsService smsService;

    @PostMapping("/send")
    public Result<String> send(@RequestParam String phone) {
        smsService.sendSms(phone);
        return Result.ok("短信发送成功");
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        return Result.ok(smsService.login(loginRequest));
    }
}
