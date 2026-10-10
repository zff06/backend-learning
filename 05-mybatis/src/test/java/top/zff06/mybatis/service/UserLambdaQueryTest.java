package top.zff06.mybatis.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import top.zff06.mybatis.dto.UserQuery;
import top.zff06.mybatis.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 条件查询测试：LambdaQueryWrapper 的各类条件、排序、分页、单列、JSON、分组统计
 * <p>
 * 与 data.sql 保持一致（未删除 5 条）：
 * admin(1,gender=1,age35,GOLD,vip1,points12000,balance99999.99)
 * zhangsan(2,gender=1,age20,BRONZE,vip0,points120)
 * lisi(3,gender=2,age26,SILVER,vip1,points2600,balance1280.00)
 * wangwu(4,gender=1,age31,GOLD,vip1,points8800,balance4300.75)
 * zhaoliu(5,gender=0,age24,BRONZE,vip0,deleted=1)
 * sunqi(6,gender=2,age22,SILVER,vip0,status=0,balance640.00)
 */
@SpringBootTest
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"})
class UserLambdaQueryTest {

    @Autowired
    private UserService userService;

    private UserQuery query() {
        return new UserQuery();
    }

    @Test
    @DisplayName("空条件等价于查全部")
    void listByCondition_shouldReturnAllWhenEmpty() {
        assertEquals(5, userService.listByCondition(null).size());
        assertEquals(5, userService.listByCondition(query()).size());
    }

    @Test
    @DisplayName("按性别等值筛选")
    void listByCondition_shouldFilterByGender() {
        UserQuery query = query();
        query.setGender(2);

        List<User> users = userService.listByCondition(query);
        assertEquals(2, users.size(), "lisi(3) 和 sunqi(6)");
        assertEquals("sunqi", users.get(0).getUsername(), "默认按 id 倒序，id 大的在前");
        assertEquals("lisi", users.get(1).getUsername());
    }

    @Test
    @DisplayName("多条件组合：性别 + 年龄区间 + 会员等级")
    void listByCondition_shouldCombineConditions() {
        UserQuery query = query();
        query.setGender(1);
        query.setMinAge(25);
        query.setMaxAge(30);
        query.setMemberLevel("GOLD");

        // admin 是 35 岁、wangwu 31 岁，都在 25~30 之外；gender=1 又只剩这两人，
        // 所以三个条件同时生效时结果必然为空 —— 空结果恰好证明条件没有被静默丢弃
        assertTrue(userService.listByCondition(query).isEmpty(),
                "gender=1 的 admin(35)/zhangsan(20)/wangwu(31) 都不满足 age 25~30");

        // 只放开年龄上界，admin 就应命中，说明区间条件确实参与了 SQL
        query.setMaxAge(40);
        List<User> users = userService.listByCondition(query);
        assertEquals(2, users.size(), "GOLD 且 gender=1 且 age>=25 的是 wangwu(31) 和 admin(35)");
        List<String> names = users.stream().map(User::getUsername).toList();
        assertTrue(names.containsAll(List.of("admin", "wangwu")));
    }

    @Test
    @DisplayName("关键字同时匹配 username 与 nickname")
    void listByCondition_keywordMatchesBothColumns() {
        // “张”只命中 zhangsan 的 nickname「张三」，username 是拼音不含「张」
        UserQuery byNickname = query();
        byNickname.setKeyword("张");
        List<User> nicknameHit = userService.listByCondition(byNickname);
        assertEquals(1, nicknameHit.size());
        assertEquals("zhangsan", nicknameHit.get(0).getUsername());

        // “李”只命中 lisi 的 nickname「李四」
        UserQuery byNickname2 = query();
        byNickname2.setKeyword("李");
        assertEquals("lisi", userService.listByCondition(byNickname2).get(0).getUsername());

        // 拼音关键字命中的是 username
        UserQuery byUsername = query();
        byUsername.setKeyword("wangwu");
        assertEquals(1, userService.listByCondition(byUsername).size());
    }

    @Test
    @DisplayName("按 vip 布尔条件筛选")
    void listByCondition_shouldFilterByVip() {
        UserQuery query = query();
        query.setVip(true);

        List<User> users = userService.listByCondition(query);
        assertEquals(3, users.size(), "admin(1) / lisi(3) / wangwu(4)");
        assertTrue(users.stream().allMatch(User::getVip));
    }

    @Test
    @DisplayName("按状态筛选")
    void listByCondition_shouldFilterByStatus() {
        UserQuery query = query();
        query.setStatus(0);

        List<User> users = userService.listByCondition(query);
        assertEquals(1, users.size());
        assertEquals("sunqi", users.get(0).getUsername());
    }

    @Test
    @DisplayName("条件分页：条件先过滤再分页")
    void pageByCondition_shouldPaginateFilteredResult() {
        var page = userService.pageByCondition(null, 1, 2);

        assertEquals(5, page.getTotal());
        assertEquals(3, page.getPages());
        assertEquals(2, page.getRecords().size());
        assertEquals(6L, page.getRecords().get(0).getId(), "默认按 id 倒序，第一条是 id 最大的");
    }

    @Test
    @DisplayName("模糊查询：or 会被括号包住，不会污染其他条件")
    void searchByKeyword_shouldWrapOrInParentheses() {
        List<User> byUsername = userService.searchByKeyword("zhangsan");
        assertEquals("zhangsan", byUsername.get(0).getUsername());

        List<User> byNickname = userService.searchByKeyword("王");
        assertEquals("wangwu", byNickname.get(0).getUsername());

        assertTrue(userService.searchByKeyword("没有这个人").isEmpty());
    }

    @Test
    @DisplayName("区间查询：按年龄升序")
    void listByAgeRange_shouldReturnAscending() {
        List<User> users = userService.listByAgeRange(20, 25);

        assertEquals(2, users.size());
        assertEquals("zhangsan", users.get(0).getUsername(), "20 岁在前");
        assertEquals("sunqi", users.get(1).getUsername(), "22 岁在后");
    }

    @Test
    @DisplayName("区间查询：只传一边时另一边不参与 SQL")
    void listByAgeRange_shouldTolerateNullBound() {
        assertEquals(5, userService.listByAgeRange(null, null).size());
        assertTrue(userService.listByAgeRange(30, null).stream()
                .allMatch(user -> user.getAge() >= 30));
    }

    @Test
    @DisplayName("取前 N 条：按积分倒序，非法值回退默认值")
    void listTop_shouldReturnTopByPoints() {
        List<User> users = userService.listTop(2);

        assertEquals(2, users.size());
        assertEquals("admin", users.get(0).getUsername(), "积分 12000 最高");
        assertEquals("wangwu", users.get(1).getUsername(), "积分 8800 次之");
        assertEquals(3, userService.listTop(null).size(), "null 时回退默认 3 条");
    }

    @Test
    @DisplayName("单列查询：只取 username")
    void listUsernames_shouldReturnSingleColumn() {
        assertEquals(List.of("admin", "zhangsan", "lisi", "wangwu", "sunqi"),
                userService.listUsernames(), "zhaoliu 已逻辑删除，不在列表中");
    }

    @Test
    @DisplayName("JSON 列查询：JSON_CONTAINS 命中标签")
    void listByTag_shouldMatchJsonArray() {
        List<User> javaUsers = userService.listByTag("Java");
        assertEquals(1, javaUsers.size());
        assertEquals("zhangsan", javaUsers.get(0).getUsername());

        // zhaoliu 的 tags 也是 ["测试"]，但它 deleted=1，逻辑删除条件先把它过滤掉了
        List<User> testTag = userService.listByTag("测试");
        assertEquals(1, testTag.size());
        assertEquals("sunqi", testTag.get(0).getUsername());

        assertTrue(userService.listByTag("不存在的标签").isEmpty());
    }

    @Test
    @DisplayName("分组统计：聚合值与分组结果")
    void statistics_shouldAggregate() {
        Map<String, Object> result = userService.statistics();

        assertEquals(5L, ((Number) result.get("totalCount")).longValue());
        // 99999.99 + 88.50 + 1280.00 + 4300.75 + 640.00 = 106309.24（zhaoliu 已逻辑删除不计入）
        assertEquals(0, new BigDecimal(result.get("totalBalance").toString())
                .compareTo(new BigDecimal("106309.24")));
        assertEquals(35, ((Number) result.get("maxAge")).intValue());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> genderCount = (List<Map<String, Object>>) result.get("genderCount");
        assertNotNull(genderCount);
        // 只有两组：唯一 gender=0 的 zhaoliu 已逻辑删除，被 deleted=0 条件过滤掉
        assertEquals(2, genderCount.size(), "未删除数据里只有 gender 1 和 2");
        // 按 gender 升序：1 -> 3 人，2 -> 2 人
        assertEquals(1, ((Number) genderCount.get(0).get("gender")).intValue());
        assertEquals(3L, ((Number) genderCount.get(0).get("cnt")).longValue());
        assertEquals(2, ((Number) genderCount.get(1).get("gender")).intValue());
        assertEquals(2L, ((Number) genderCount.get(1).get("cnt")).longValue());
    }
}
