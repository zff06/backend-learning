package top.zff06.toolkit.dto;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description QrStatusResponse
 **/
public record QrStatusResponse(String ticket, String state, String phone, String token) {
}
