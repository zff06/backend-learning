package top.zff06.toolkit.dto;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description ShortLinkStats
 **/
public record ShortLinkStats(String code, String url, long clicks, long createdAt, long expireAt) {
}
