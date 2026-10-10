package top.zff06.mybatis.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口层测试：逐个覆盖文档第 6 节的接口验证表
 * <p>
 * 用 MockMvc 直连本地 MySQL（与 application.yml 同一套数据源），不占用真实端口。
 * 与 data.sql 保持一致（未删除 5 条）：
 * admin(1) / zhangsan(2) / lisi(3) / wangwu(4) / zhaoliu(5，deleted=1) / sunqi(6，status=0)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"})
class UserApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /* ---------- 增 ---------- */

    @Test
    @DisplayName("POST /users：新增返回带 id、createTime 的用户，password 不出现在响应里")
    void create_shouldReturnCreatedUser() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"zhouba\",\"age\":28,\"tags\":[\"Java\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("zhouba"))
                .andExpect(jsonPath("$.memberLevel").value("BRONZE"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.createTime").isNotEmpty())
                .andExpect(jsonPath("$.tags[0]").value("Java"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /users/batch：批量新增返回成功条数")
    void createBatch_shouldReturnCount() throws Exception {
        mockMvc.perform(post("/users/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"username\":\"b1\",\"age\":20},{\"username\":\"b2\",\"age\":21}]"))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    /* ---------- 查 ---------- */

    @Test
    @DisplayName("GET /users/{id}：返回完整用户，含 tags 数组")
    void getById_shouldReturnEntityWithTags() throws Exception {
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.nickname").value("超级管理员"))
                .andExpect(jsonPath("$.tags.length()").value(3));
    }

    @Test
    @DisplayName("GET /users：条件列表只返回符合条件的数据")
    void list_shouldFilterByQuery() throws Exception {
        // buildCondition 里是 orderByDesc(User::getId)，所以按 id 倒序返回
        mockMvc.perform(get("/users").param("gender", "1")
                        .param("minAge", "20").param("maxAge", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("zhangsan"));
    }

    @Test
    @DisplayName("GET /users/page：返回 records/total/current/pages")
    void page_shouldReturnPaginationFields() throws Exception {
        mockMvc.perform(get("/users/page").param("current", "1").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.current").value(1))
                .andExpect(jsonPath("$.pages").value(2))
                .andExpect(jsonPath("$.records.length()").value(3));
    }

    @Test
    @DisplayName("GET /users/keyword：命中 username 或 nickname")
    void keyword_shouldMatchBothColumns() throws Exception {
        // “张”只命中 zhangsan 的 nickname「张三」
        mockMvc.perform(get("/users/keyword").param("keyword", "张"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nickname").value("张三"));

        // 拼音关键字命中 lisi 的 username
        mockMvc.perform(get("/users/keyword").param("keyword", "lisi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("lisi"));
    }

    @Test
    @DisplayName("GET /users/age-range：按年龄升序返回")
    void ageRange_shouldReturnAscending() throws Exception {
        mockMvc.perform(get("/users/age-range").param("minAge", "20").param("maxAge", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].age").value(20))
                .andExpect(jsonPath("$[1].age").value(22));
    }

    @Test
    @DisplayName("GET /users/top：按积分倒序取前 N 条")
    void top_shouldReturnTopByPoints() throws Exception {
        mockMvc.perform(get("/users/top").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[1].username").value("wangwu"));
    }

    @Test
    @DisplayName("GET /users/usernames：返回字符串数组")
    void usernames_shouldReturnStringArray() throws Exception {
        mockMvc.perform(get("/users/usernames"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0]").value("admin"));
    }

    @Test
    @DisplayName("GET /users/tag：JSON_CONTAINS 命中标签")
    void byTag_shouldMatchJsonArray() throws Exception {
        mockMvc.perform(get("/users/tag").param("tag", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("zhangsan"));
    }

    @Test
    @DisplayName("GET /users/exists：用户名是否存在")
    void exists_shouldReturnBoolean() throws Exception {
        mockMvc.perform(get("/users/exists").param("username", "admin"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        mockMvc.perform(get("/users/exists").param("username", "nobody"))
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("GET /users/count：按性别计数，不传则统计全部")
    void count_shouldCountByGender() throws Exception {
        mockMvc.perform(get("/users/count").param("gender", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));

        mockMvc.perform(get("/users/count"))
                .andExpect(content().string("5"));
    }

    @Test
    @DisplayName("GET /users/statistics：返回汇总与分组统计")
    void statistics_shouldReturnAggregates() throws Exception {
        mockMvc.perform(get("/users/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(5))
                .andExpect(jsonPath("$.maxAge").value(35))
                .andExpect(jsonPath("$.totalBalance").exists())
                // 只有两组：唯一 gender=0 的 zhaoliu 已逻辑删除
                .andExpect(jsonPath("$.genderCount.length()").value(2));
    }

    /* ---------- 改 ---------- */

    @Test
    @DisplayName("PUT /users/{id}：更新后返回最新数据")
    void update_shouldReturnUpdatedUser() throws Exception {
        mockMvc.perform(put("/users/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"zhangsan-new\",\"age\":21}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.nickname").value("zhangsan-new"))
                .andExpect(jsonPath("$.age").value(21));
    }

    @Test
    @DisplayName("PUT /users/{id}/status：按状态条件更新")
    void updateStatus_shouldReturnTrue() throws Exception {
        mockMvc.perform(put("/users/2/status").param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT status FROM t_user WHERE id = 2", Integer.class));
    }

    @Test
    @DisplayName("POST /users/{id}/recharge：余额累加，version 加一")
    void recharge_shouldAddBalance() throws Exception {
        // lisi(3) 初始余额 1280.00，充值 100.25 后为 1380.25
        mockMvc.perform(post("/users/3/recharge").param("amount", "100.25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.balance").value(1380.25))
                .andExpect(jsonPath("$.version").value(1));
    }

    /* ---------- 删 ---------- */

    @Test
    @DisplayName("DELETE /users/{id}：逻辑删除，之后查不到")
    void delete_shouldLogicallyRemove() throws Exception {
        mockMvc.perform(delete("/users/4"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        mockMvc.perform(get("/users/4"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT deleted FROM t_user WHERE id = 4", Integer.class));
    }

    @Test
    @DisplayName("DELETE /users/batch：按 id 集合批量删除")
    void deleteBatch_shouldReturnTrue() throws Exception {
        mockMvc.perform(delete("/users/batch").param("ids", "2,3"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_user WHERE id IN (2, 3) AND deleted = 1", Integer.class));
    }

    @Test
    @DisplayName("DELETE /users/status/{status}：按状态条件删除")
    void deleteByStatus_shouldReturnTrue() throws Exception {
        // 只有 sunqi(6) 是 status=0
        mockMvc.perform(delete("/users/status/0"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        mockMvc.perform(get("/users/6")).andExpect(content().string(""));
        mockMvc.perform(get("/users/1"))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    /* ---------- 事务 ---------- */

    @Test
    @DisplayName("事务回滚：POST /users 年龄超范围，库里不落数据")
    void createOutOfRangeAge_shouldRollback() {
        // 请求体内抛出未捕获异常，MockMvc 会把它重新抛出来
        assertThrows(Exception.class, () -> mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"out_of_range\",\"age\":200}")));

        assertEquals(0, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM t_user WHERE username = 'out_of_range'", Integer.class),
                "先写库再抛异常，事务应把这条记录回滚掉");
    }
}
