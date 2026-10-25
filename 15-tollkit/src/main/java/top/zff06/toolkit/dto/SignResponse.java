package top.zff06.toolkit.dto;

/**
 * 签名示例
 *
 * @param appId     应用标识
 * @param timestamp 秒级时间戳
 * @param nonce     随机串
 * @param body      请求体原文
 * @param canonical 参与签名的字符串
 * @param sign      签名结果
 * @auther zff06
 * @data 2026/10/22
 */
public record SignResponse(String appId, String timestamp, String nonce,
                           String body, String canonical, String sign) {
}
