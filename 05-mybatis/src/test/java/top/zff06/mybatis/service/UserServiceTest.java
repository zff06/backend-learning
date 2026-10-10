package top.zff06.mybatis.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import top.zff06.mybatis.dto.UserQuery;
import top.zff06.mybatis.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 服务层测试：增删改查、自动填充、逻辑删除、乐观锁、事务
 * <p>
 * 每个用例执行前都用 schema.sql + data.sql 重建表并写入示例数据，
 * 与 data.sql 完全一致：admin(1) / zhangsan(2) / lisi(3) / wangwu(4) / zhaoliu(5，deleted=1,gender=0) / sunqi(6，status=0)
 */
@SpringBootTest
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"})
class UserServiceTest {

    private static final String SHA256_123456 =
            "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";

    /** 未逻辑删除的行数：data.sql 共 6 条，其中 zhaoliu(5) 是 deleted = 1 */
    private static final int ACTIVE_USER_COUNT = 5;

    @Autowired
    private UserService userService;

    /** 用来绕过逻辑删除、直接看物理行 */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User newUser(String username, Integer age) {
        User user = new User();
        user.setUsername(username);
        user.setAge(age);
        return user;
    }

    /* ---------- 增 ---------- */

    @Test
    @DisplayName("新增：未传的字段补默认值，密码加密，审计字段自动填充")
    void createUser_shouldFillDefaultsAndAuditFields() {
        User user = newUser("newbie", 20);

        assertTrue(userService.createUser(user));

        assertNotNull(user.getId(), "主键应回填到实体");
        assertEquals("BRONZE", user.getMemberLevel());
        assertEquals(0, user.getGender().intValue());
        assertEquals(1, user.getStatus().intValue());
        assertEquals(0, user.getPoints().intValue());
        assertFalse(user.getVip());
        assertEquals(0, user.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(SHA256_123456, user.getPassword(), "密码应被换成 SHA-256 摘要");
        assertNotNull(user.getCreateTime());
        assertNotNull(user.getUpdateTime());
        assertEquals(0, user.getVersion().intValue(), "新增时版本号由 MetaObjectHandler 填 0");
        assertEquals(0, user.getDeleted().intValue(), "新增时逻辑删除标记由 MetaObjectHandler 填 0");
    }

    @Test
    @DisplayName("新增：用户名重复违反唯一约束")
    void createUser_shouldFailOnDuplicateUsername() {
        assertThrows(DataIntegrityViolationException.class,
                () -> userService.createUser(newUser("admin", 20)));
    }

    @Test
    @DisplayName("事务：写库后校验失败应整体回滚")
    void createUser_shouldRollbackWhenAgeOutOfRange() {
        assertThrows(RuntimeException.class,
                () -> userService.createUser(newUser("rollback_user", 200)));

        assertFalse(userService.existsUsername("rollback_user"), "异常前插入的数据应被回滚");
    }

    @Test
    @DisplayName("批量新增：一次写入多条，默认值同样生效")
    void createUserBatch_shouldInsertAll() {
        assertTrue(userService.createUserBatch(List.of(
                newUser("batch1", 20), newUser("batch2", 21), newUser("batch3", 22))));

        assertTrue(userService.existsUsername("batch1"));
        assertTrue(userService.existsUsername("batch3"));
        assertEquals(SHA256_123456, userService.listByCondition(
                queryOf("batch2")).get(0).getPassword());
    }

    @Test
    @DisplayName("批量新增：空集合直接返回 false")
    void createUserBatch_shouldReturnFalseOnEmpty() {
        assertFalse(userService.createUserBatch(List.of()));
    }

    /* ---------- 查 ---------- */

    @Test
    @DisplayName("查询：按主键能取到完整对象，JSON 列正常反序列化")
    void getUser_shouldReturnFullEntity() {
        User user = userService.getUser(1L);

        assertNotNull(user);
        assertEquals("admin", user.getUsername());
        assertEquals("超级管理员", user.getNickname());
        assertEquals(List.of("管理员", "全栈开发", "技术负责人"), user.getTags(),
                "tags 是 JSON 列，靠 autoResultMap 反序列化");
        assertEquals("GOLD", user.getMemberLevel());
    }

    @Test
    @DisplayName("查询：主键不存在返回 null，不抛异常")
    void getUser_shouldReturnNullWhenAbsent() {
        assertNull(userService.getUser(9999L));
    }

    @Test
    @DisplayName("查询：list 只返回未逻辑删除的数据")
    void listUsers_shouldExcludeLogicallyDeleted() {
        List<User> users = userService.listUsers();

        assertEquals(ACTIVE_USER_COUNT, users.size(), "示例数据 6 条，其中 zhaoliu(5) deleted=1 应被过滤");
        assertNull(userService.getUser(5L), "逻辑删除的行查不到");
    }

    @Test
    @DisplayName("查询：分页返回 total / pages / records")
    void pageUsers_shouldPaginate() {
        var page = userService.pageUsers(1, 3);

        assertEquals(ACTIVE_USER_COUNT, page.getTotal());
        assertEquals(2, page.getPages());
        assertEquals(3, page.getRecords().size());
    }

    /* ---------- 改 ---------- */

    @Test
    @DisplayName("更新：更新后 update_time 会刷新，version 随实体走")
    void modifyUser_shouldRefreshUpdateTime() {
        User user = userService.getUser(2L);
        LocalDateTime before = user.getUpdateTime();

        user.setNickname("张三改名");
        user.setAge(21);
        assertTrue(userService.modifyUser(user));

        User after = userService.getUser(2L);
        assertEquals("张三改名", after.getNickname());
        assertTrue(after.getUpdateTime().isAfter(before), "updateTime 应被刷新而不是写回旧值");
    }

    @Test
    @DisplayName("更新：缺主键或记录不存在都抛 IllegalArgumentException")
    void modifyUser_shouldValidateIdFirst() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.modifyUser(newUser("nobody", 20)));

        User notExist = newUser("nobody", 20);
        notExist.setId(9999L);
        assertThrows(IllegalArgumentException.class, () -> userService.modifyUser(notExist));
    }

    @Test
    @DisplayName("更新：LambdaUpdateWrapper 按条件改指定字段")
    void modifyUserStatus_shouldUpdateByCondition() {
        assertTrue(userService.modifyUserStatus(1L, 0));

        assertEquals(0, userService.getUser(1L).getStatus().intValue());
    }

    @Test
    @DisplayName("充值：余额累加，乐观锁版本号加一")
    void recharge_shouldAddBalanceAndBumpVersion() {
        User before = userService.getUser(6L);
        assertEquals(0, before.getBalance().compareTo(new BigDecimal("640.00")));

        assertTrue(userService.recharge(6L, new BigDecimal("100.25")));

        User after = userService.getUser(6L);
        assertEquals(0, after.getBalance().compareTo(new BigDecimal("740.25")));
        assertEquals(before.getVersion() + 1, after.getVersion().intValue());
    }

    @Test
    @DisplayName("充值：金额非法或用户不存在都拒绝")
    void recharge_shouldRejectInvalidAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.recharge(1L, new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> userService.recharge(1L, null));
        assertThrows(IllegalArgumentException.class,
                () -> userService.recharge(9999L, new BigDecimal("10")));
    }

    @Test
    @DisplayName("乐观锁：同一个实体的陈旧副本更新会被拦下")
    void optimisticLock_shouldRejectStaleEntity() {
        User first = userService.getUser(2L);
        User stale = userService.getUser(2L);

        first.setNickname("第一次改");
        assertTrue(userService.modifyUser(first), "版本匹配，更新成功");

        stale.setNickname("第二次改");
        assertFalse(userService.modifyUser(stale), "版本号已过期，WHERE version=? 不匹配，影响行数为 0");
    }

    /* ---------- 删 ---------- */

    @Test
    @DisplayName("删除：逻辑删除后查不到，物理行仍在且 deleted=1")
    void removeUser_shouldLogicallyDelete() {
        assertTrue(userService.removeUser(4L));

        assertNull(userService.getUser(4L));
        assertEquals(1, jdbcTemplate.queryForObject(
                        "SELECT deleted FROM t_user WHERE id = ?", Integer.class, 4L),
                "物理行没有被删除，只是标记 deleted=1");
    }

    @Test
    @DisplayName("删除：批量删除与按状态删除")
    void removeBatchAndByStatus_shouldWork() {
        assertTrue(userService.removeUserBatch(List.of(2L, 3L)));
        assertNull(userService.getUser(2L));
        assertNull(userService.getUser(3L));

        assertTrue(userService.removeUserByStatus(0), "删掉 status=0 的 sunqi(6)");
        assertNull(userService.getUser(6L));
        assertNotNull(userService.getUser(1L), "status=1 的 admin 不受影响");
    }

    @Test
    @DisplayName("删除：空 id 集合返回 false")
    void removeUserBatch_shouldReturnFalseOnEmpty() {
        assertFalse(userService.removeUserBatch(List.of()));
    }

    /* ---------- 计数与判断 ---------- */

    @Test
    @DisplayName("存在判断与计数")
    void existsAndCount_shouldWork() {
        assertTrue(userService.existsUsername("admin"));
        assertFalse(userService.existsUsername("nobody"));

        assertEquals(3, userService.countByGender(1), "admin(1) / zhangsan(2) / wangwu(4)");
        assertEquals(ACTIVE_USER_COUNT, userService.countByGender(null), "不传性别即统计全部未删除数据");
    }

    private UserQuery queryOf(String keyword) {
        UserQuery query = new UserQuery();
        query.setKeyword(keyword);
        return query;
    }
}
