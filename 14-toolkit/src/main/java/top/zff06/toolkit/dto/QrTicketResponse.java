package top.zff06.toolkit.dto;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description QrTicketResponse
 **/
public record QrTicketResponse(String ticket, String content, String image, long expireSeconds) {
}
