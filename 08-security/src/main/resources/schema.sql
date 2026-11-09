-- =====================================================================
-- 08-security：账号与角色
-- 只保留文档 5.1 节的两张表。启动时 mode=always，先清掉旧结构再重建，
-- 所以这里的 DROP 不能省——表结构改过一次之后，残留的旧列会让 INSERT 直接失败。
-- =====================================================================

-- 清掉历史遗留：早期版本的表名与练习用的辅助表
DROP TABLE IF EXISTS sec_task;
DROP TABLE IF EXISTS sec_user_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;

DROP TABLE IF EXISTS sec_role;
DROP TABLE IF EXISTS sec_user;

CREATE TABLE sec_user
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL UNIQUE COMMENT '登录名，密码登录用它',
    phone       VARCHAR(20) DEFAULT NULL UNIQUE COMMENT '手机号，短信登录用它',
    password    VARCHAR(128) NOT NULL COMMENT '带算法前缀的密文，如 {bcrypt}$2a$10$...',
    nickname    VARCHAR(64) DEFAULT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE COMMENT 'false 时登录抛 DisabledException',
    locked      BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'true 时登录抛 LockedException',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '账号';

CREATE TABLE sec_role
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    role_code  VARCHAR(64) NOT NULL,
    grant_type VARCHAR(8)  NOT NULL DEFAULT 'ROLE',
    UNIQUE KEY uk_user_role (user_id, role_code),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '账号的角色与权限点';
