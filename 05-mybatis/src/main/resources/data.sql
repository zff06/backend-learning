-- 初始化数据：覆盖不同性别、等级、状态、逻辑删除、会员与各类字段的典型取值
-- password 字段统一存 SHA-256("123456") 的十六进制摘要
-- 表由 schema.sql 先删后建，这里直接 INSERT 即可，重复启动不会累积脏数据
INSERT INTO t_user (id, username, password, nickname, gender, age, member_level, points, balance, score,
                    phone, email, avatar, intro, tags, birthday, work_time, vip, status,
                    last_login_time, create_time, update_time, deleted, version)
VALUES (1, 'admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '超级管理员',
        1, 35, 'GOLD', 12000, 99999.99, 4.9, '13800000001', 'admin@mqxu.top', 'https://cdn.mqxu.top/a/admin.png',
        '系统内置账号，拥有全部权限。', '["管理员","全栈开发","技术负责人"]', '1991-05-20', '09:00:00', 1, 1,
        '2026-10-09 08:30:00', '2026-09-01 10:00:00', '2026-10-09 08:30:00', 0, 0),

       (2, 'zhangsan', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '张三',
        1, 20, 'BRONZE', 120, 88.50, 4.2, '13800000002', 'zhangsan@mqxu.top', 'https://cdn.mqxu.top/a/zhangsan.png',
        '在读大二，正在学 Spring Boot 与 MyBatis-Plus。', '["学生","Java","篮球"]', '2006-03-15', '08:30:00', 0, 1,
        '2026-10-08 21:12:00', '2026-09-05 09:20:00', '2026-10-08 21:12:00', 0, 0),

       (3, 'lisi', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '李四',
        2, 26, 'SILVER', 2600, 1280.00, 4.5, '13800000003', 'lisi@mqxu.top', 'https://cdn.mqxu.top/a/lisi.png',
        '前端工程师，业余写 Vue 组件库。', '["前端","Vue","摄影"]', '2000-11-02', '09:30:00', 1, 1,
        '2026-10-09 19:40:00', '2026-09-10 14:05:00', '2026-10-09 19:40:00', 0, 0),

       (4, 'wangwu', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '王五',
        1, 31, 'GOLD', 8800, 4300.75, 4.8, '13800000004', 'wangwu@mqxu.top', 'https://cdn.mqxu.top/a/wangwu.png',
        '后端架构师，关注高并发与数据库性能。', '["架构","MySQL","跑步"]', '1995-07-08', '08:00:00', 1, 1,
        '2026-10-07 07:55:00', '2026-09-12 11:30:00', '2026-10-07 07:55:00', 0, 0),

       (5, 'zhaoliu', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '赵六',
        0, 24, 'BRONZE', 300, 12.30, 3.6, '13800000005', 'zhaoliu@mqxu.top', 'https://cdn.mqxu.top/a/zhaoliu.png',
        '这条记录 deleted=1，用来演示逻辑删除查不到。', '["测试"]', '2002-01-19', '10:00:00', 0, 1,
        '2026-09-28 16:20:00', '2026-09-15 09:00:00', '2026-09-28 16:20:00', 1, 0),

       (6, 'sunqi', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '孙七',
        2, 22, 'SILVER', 1500, 640.00, 4.0, '13800000006', 'sunqi@mqxu.top', 'https://cdn.mqxu.top/a/sunqi.png',
        '这条记录 status=0，用来演示按状态过滤。', '["测试","产品"]', '2004-09-30', '09:15:00', 0, 0,
        '2026-10-01 12:00:00', '2026-09-18 10:45:00', '2026-10-01 12:00:00', 0, 0);

-- 课程表初始化数据：3 条典型记录，用于练习 saveBatch（批量新增）与 removeByIds（按 id 批量删除）
-- t_course 没有 deleted 列，这里的删除是物理删除，和 t_user 的逻辑删除形成对比
-- id 显式指定，方便演示 removeByIds 时直接知道要删哪几条
-- stock 是练习5（下单扣库存）的库存字段，给足余量方便演示"库存足够"与"库存不足"两种路径
INSERT INTO t_course (id, name, teacher, credit, stock)
VALUES (1, 'Java 程序设计', '张伟', 4, 100),
       (2, 'Spring Boot 实战', '李明', 3, 50),
       (3, 'MySQL 数据库原理', '王芳', 2, 30);

-- 订单表初始化数据：3 条典型记录
-- amount = 学分 × 99 × 数量；create_time / update_time 是 NOT NULL，脚本里必须显式给值
-- id=3 这条 deleted=1，用来演示逻辑删除后查询查不到、但物理行仍在
INSERT INTO t_order (id, order_no, course_id, quantity, amount, status, create_time, update_time, deleted)
VALUES (1, 'ORD20261001A001', 1, 2, 792.00, 1, '2026-10-01 10:15:00', '2026-10-01 10:15:00', 0),
       (2, 'ORD20261002B002', 2, 1, 297.00, 0, '2026-10-02 14:30:00', '2026-10-02 14:30:00', 0),
       (3, 'ORD20261003C003', 3, 3, 594.00, 1, '2026-10-03 09:05:00', '2026-10-03 09:05:00', 1);
