package top.zff06.toolkit.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.zff06.toolkit.common.Result;
import top.zff06.toolkit.dto.CaptchaResponse;
import top.zff06.toolkit.service.CaptchaService;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description CaptchaController
 **/
@RestController
@RequestMapping("/toolkit/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    /**
     * 生成验证码
     *
     * @param type line（默认）/ circle / shear / gif / math
     */
    @GetMapping
    public Result<CaptchaResponse> create(@RequestParam(defaultValue = "line") String type) {
        return Result.ok(captchaService.create(type));
    }

    /**
     * 校验验证码，成功后该 captchaId 立即失效
     */
    @PostMapping("/verify")
    public Result<Boolean> verify(@Valid @RequestBody VerifyRequest request) {
        captchaService.verify(request.captchaId(), request.code());
        return Result.ok("验证通过", true);
    }

    public record VerifyRequest(
            @NotBlank(message = "captchaId 不能为空") String captchaId,
            @NotBlank(message = "验证码不能为空") String code) {
    }
}
