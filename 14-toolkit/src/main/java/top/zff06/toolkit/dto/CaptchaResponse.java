package top.zff06.toolkit.dto;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description CaptchaResponse 验证码下发结果
 **/
public record CaptchaResponse(String captchaId, String image, long expireSeconds, String type, String code) {
}