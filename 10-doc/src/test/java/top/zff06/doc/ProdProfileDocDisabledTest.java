package top.zff06.doc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 练习 5：限制文档的访问（选方案一：springdoc 开关）
 * <p>
 * 用 prod profile 起一遍上下文，断言 /v3/api-docs 已被关掉、业务接口照常。
 * 这样"生产环境不公开文档"这条约定就有测试兜着，不会因为改配置被人手滑放开。
 * <p>
 * 注意断言口径：本模块有 GlobalExceptionHandler 接 NoResourceFoundException，
 * HTTP 状态仍是 200，真正的信号是响应体 code=40400 / message=资源不存在。
 * 如果只写 status().isNotFound() 会误判成"文档没关掉"。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description ProdProfileDocDisabledTest
 **/
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProdProfileDocDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    @DisplayName("prod profile 下 /v3/api-docs 应不可用（响应里没有 paths）")
    void apiDocsShouldBeUnavailable() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode node = jsonMapper.readTree(body);
        assertNull(node.get("paths"), "文档没关掉：响应里还有 paths");
    }

    @Test
    @DisplayName("prod profile 下 /swagger-ui.html 也应不可用")
    void swaggerUiShouldBeUnavailable() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("关文档不影响业务接口")
    void businessApiShouldStillWork() throws Exception {
        mockMvc.perform(get("/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
