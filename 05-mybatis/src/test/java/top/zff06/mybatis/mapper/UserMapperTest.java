package top.zff06.mybatis.mapper;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import top.zff06.mybatis.entity.User;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mapper 层测试：直接验证 BaseMapper 的通用 CRUD，不经过 Service
 * <p>
 * 与其它测试类一样用 schema.sql + data.sql 重建数据，避免污染真实库、
 * 也避免删掉 id=4 之后影响其它测试类的数据基线。
 */
@SpringBootTest
@Slf4j
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"})
class UserMapperTest {

    /** SHA-256("123456")，data.sql 里所有示例用户用的就是这个摘要 */
    private static final String SHA256_123456 =
            "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";

    @Resource
    private UserMapper userMapper;

    @Test
    void testInsert() {
        User user = new User();
        user.setUsername("Mikasa");
        user.setAge(19);
        // 注意：这里直接用 Mapper 插入，不经过 UserServiceImpl.applyDefaults()，
        // 所以 password 必须自己给值——表里 password 是 CHAR(64) NOT NULL 且无默认值
        user.setPassword(SHA256_123456);
        user.setCreateTime(LocalDateTime.now());
        int result = userMapper.insert(user);
        assertEquals(1, result);
        assertNotNull(user.getId(), "自增主键应回填到实体");
        log.info("{} 条数据插入成功，id = {}", result, user.getId());
    }

    @Test
    void testSearch() {
        // data.sql 里 id=3 是 lisi
        User user = userMapper.selectById(3);
        assertNotNull(user);
        assertEquals("lisi", user.getUsername());
        assertEquals("李四", user.getNickname());
        log.info("用户信息: {}", user);
    }

    @Test
    void testDelete() {
        // 配了 @TableLogic，这里其实是逻辑删除：UPDATE ... SET deleted=1
        int result = userMapper.deleteById(4);
        assertEquals(1, result);
        assertNull(userMapper.selectById(4), "逻辑删除后查询自动追加 deleted=0，查不到");
        log.info("{} 条数据删除成功", result);
    }
}
