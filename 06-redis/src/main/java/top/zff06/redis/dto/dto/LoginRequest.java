package top.zff06.redis.dto.dto;

import lombok.Data;

/**
 * @auther zff06
 * @data 2026/10/19
 * @description LoginRequest
 **/
@Data
public class LoginRequest {
    private String phone;
    private String code;
}
