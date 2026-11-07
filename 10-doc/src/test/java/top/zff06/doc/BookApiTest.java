package top.zff06.doc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import top.zff06.doc.common.ErrorCode;
import top.zff06.doc.common.Result;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文档写了什么，接口就得真返回什么
 * <p>
 * 这里用 Jackson 3（Boot 4 默认的 tools.jackson）读响应体，
 * 顺手验证了"JSON 解析失败"这类问题不该怪接口。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description BookApiTest
 **/
@SpringBootTest
@AutoConfigureMockMvc
class BookApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    @DisplayName("分页查询：返回列表 + 总数")
    void pageShouldWork() throws Exception {
        mockMvc.perform(get("/books").param("keyword", "Java").param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.list.length()").value(2));
    }

    @Test
    @DisplayName("按状态过滤：SOLD_OUT 只有一本")
    void filterByStatus() throws Exception {
        mockMvc.perform(get("/books").param("status", "SOLD_OUT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].status").value("SOLD_OUT"));
    }

    @Test
    @DisplayName("不存在：code=30001，与文档里 @ApiResponse 示例一致")
    void notFoundShouldMatchDoc() throws Exception {
        mockMvc.perform(get("/books/9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.BOOK_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value("图书不存在"));
    }

    @Test
    @DisplayName("校验失败：文档里声明的 required / pattern 真的会拦下来")
    void invalidBodyShouldBeRejected() throws Exception {
        String body = """
                {"title":"","author":"x","isbn":"123","price":0,"stock":-1}
                """;
        mockMvc.perform(post("/books").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("新增 + ISBN 重复 + 删除，全链路返回体结构一致")
    void createUpdateDeleteFlow() throws Exception {
        String newBook = """
                {"title":"Java 核心技术","author":"Cay S. Horstmann","isbn":"9787111636663",
                 "price":149.00,"stock":30,"status":"ON_SALE"}
                """;
        String created = mockMvc.perform(post("/books").contentType(MediaType.APPLICATION_JSON).content(newBook))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.isbn").value("9787111636663"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        JsonNode node = jsonMapper.readTree(created);
        long id = node.get("data").get("id").asLong();
        assertTrue(id > 0);

        // 同一 ISBN 再提交一次，应该命中 30002
        mockMvc.perform(post("/books").contentType(MediaType.APPLICATION_JSON).content(newBook))
                .andExpect(jsonPath("$.code").value(ErrorCode.ISBN_DUPLICATED.getCode()))
                .andExpect(jsonPath("$.message").value("ISBN 已存在"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/books/" + id))
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/books/" + id))
                .andExpect(jsonPath("$.code").value(ErrorCode.BOOK_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("演示 500：@Operation(deprecated=true) 的接口真的抛异常")
    void boomShouldReturn500Code() throws Exception {
        String json = mockMvc.perform(get("/books/boom"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        Result<?> result = jsonMapper.readValue(json, Result.class);
        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), result.getCode());
    }
}
