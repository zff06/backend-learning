package top.zff06.toolkit.dto;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description ShortLinkResponse
 **/
public record ShortLinkResponse(String code, String shortUrl, String url, long expireAt) {
}
