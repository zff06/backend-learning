-- =====================================================================
-- 08-security：六个账号 + 角色授权
-- 密码都是 123456，存的是同一段 {bcrypt} 密文。
-- {bcrypt} 是算法标识，DelegatingPasswordEncoder 按前缀选校验算法。
-- =====================================================================

INSERT INTO sec_user (id, username, phone, password, nickname, enabled, locked)
VALUES
    -- 正常账号
    (1, 'alice', '13900001111', '{bcrypt}$2a$10$C4766jheAQ0QhL3Jslfq2uyXheKoxcnXkS6lemAhqJEY40a7pLvdW', '普通用户', TRUE, FALSE),
    (2, 'bob', NULL, '{bcrypt}$2a$10$C4766jheAQ0QhL3Jslfq2uyXheKoxcnXkS6lemAhqJEY40a7pLvdW', '管理员兼审计', TRUE, FALSE),
    (3, 'carol', NULL, '{bcrypt}$2a$10$C4766jheAQ0QhL3Jslfq2uyXheKoxcnXkS6lemAhqJEY40a7pLvdW', '只有管理员', TRUE, FALSE),
    -- 两种不可用状态：禁用与锁定在业务上是两件事，所以分两列
    (4, 'dave', NULL, '{bcrypt}$2a$10$C4766jheAQ0QhL3Jslfq2uyXheKoxcnXkS6lemAhqJEY40a7pLvdW', '被禁用', FALSE, FALSE),
    (5, 'frank', NULL, '{bcrypt}$2a$10$C4766jheAQ0QhL3Jslfq2uyXheKoxcnXkS6lemAhqJEY40a7pLvdW', '被锁定', TRUE, TRUE),
    (6, 'erin', NULL, '{bcrypt}$2a$10$C4766jheAQ0QhL3Jslfq2uyXheKoxcnXkS6lemAhqJEY40a7pLvdW', '只有审计', TRUE, FALSE);

-- grant_type=ROLE 由 toAuthority() 补 ROLE_ 前缀；PERM 原样返回
INSERT INTO sec_role (user_id, role_code, grant_type)
VALUES
    -- alice：普通用户 + 一个细粒度权限点，用来验证 hasAuthority
    (1, 'USER', 'ROLE'),
    (1, 'ARTICLE_DELETE', 'PERM'),
    -- bob：两个角色都有，hasRole / hasAnyRole / hasAllRoles 都放行
    (2, 'ADMIN', 'ROLE'),
    (2, 'AUDITOR', 'ROLE'),
    -- carol：只有 ADMIN，用来验证 hasAllRoles 拒绝
    (3, 'ADMIN', 'ROLE'),
    -- dave：禁用账号，仍然有角色，证明被拒不是因为没权限
    (4, 'USER', 'ROLE'),
    -- frank：锁定账号
    (5, 'USER', 'ROLE'),
    -- erin：只有 AUDITOR，用来验证 hasAnyRole 放行
    (6, 'AUDITOR', 'ROLE');
