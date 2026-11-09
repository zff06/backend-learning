package top.zff06.toolkit.support;

import lombok.Data;

/**
 * 客户存储对象
 *
 * @auther zff06
 * @data 2026/10/22
 * @description 手机号、身份证以密文保存，密码只保存 BCrypt 哈希
 **/
@Data
public class Customer {

    private String id;
    private String name;
    private String phoneEncrypted;
    private String idCardEncrypted;
    private String passwordHash;
    private String level;
    private long createdAt;
}
