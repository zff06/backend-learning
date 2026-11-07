package top.zff06.doc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import top.zff06.doc.common.Result;
import top.zff06.doc.controller.BookController;
import top.zff06.doc.dto.BookQuery;
import top.zff06.doc.model.Book;
import top.zff06.doc.vo.PageResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 为什么接口文档也要写测试
 * <p>
 * 文档是"契约"，契约错了调用方就写错代码。手点 Swagger UI 只能证明"页面能打开"，
 * 证明不了"参数是独立的""分组没空""废弃标记还在"。这三件事都能在 MockMvc + JSON 上断言。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description ApiDocTest
 **/
@SpringBootTest
@AutoConfigureMockMvc
class ApiDocTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookController bookController;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    @DisplayName("全量文档：路径、Tag、SecurityScheme 都在")
    void apiDocsShouldContainAllPaths() throws Exception {
        String json = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(json.contains("\"/books\""), "缺失 /books");
        assertTrue(json.contains("\"/books/{id}\""), "缺失 /books/{id}");
        assertTrue(json.contains("\"/books/boom\""), "缺失 /books/boom");
        assertTrue(json.contains("\"/system/health\""), "缺失 /system/health");
        assertTrue(json.contains("\"bearer\""), "缺失 bearer 安全方案，UI 右上角不会出现 Authorize");
        assertTrue(json.contains("\"图书服务接口文档\""), "缺失文档标题");
    }

    @Test
    @DisplayName("分组要单独测：按路径切、按包切各拿到自己的接口")
    void groupsShouldBeSeparated() throws Exception {
        String bookGroup = get("/v3/api-docs/01-图书管理");
        assertTrue(bookGroup.contains("\"/books\""), "图书分组缺少 /books，检查 pathsToMatch 是否写成 /book/**");
        assertTrue(!bookGroup.contains("\"/system/health\""), "图书分组混进了系统接口");

        String systemGroup = get("/v3/api-docs/02-系统运维");
        assertTrue(systemGroup.contains("\"/system/health\""), "系统分组缺少 /system/health，检查 packagesToScan");
        assertTrue(!systemGroup.contains("\"/books\""), "系统分组混进了图书接口");
    }

    @Test
    @DisplayName("@ParameterObject 的效果可以断言：查询对象被摊平成 4 个 query 参数")
    void queryObjectShouldBeFlattened() throws Exception {
        String json = get("/v3/api-docs/01-图书管理");

        // 摊平成功的标志：keyword / status / pageNum / pageSize 各自是独立的 name + in:query
        for (String name : new String[]{"keyword", "status", "pageNum", "pageSize"}) {
            assertTrue(json.contains("\"name\":\"" + name + "\""), "参数 " + name + " 没有摊平出来");
        }
        // 没有被摊平的标志：整个对象作为一个参数出现
        assertTrue(!json.contains("\"name\":\"bookQuery\""), "查询对象没有被摊平，检查 Controller 上有没有 @ParameterObject");
        // 默认值、枚举、取值范围这些"从代码读出来的约束"也要在
        assertTrue(json.contains("\"default\":1"), "pageNum 默认值丢了");
        assertTrue(json.contains("\"enum\":[\"ON_SALE\",\"OFF_SHELF\",\"SOLD_OUT\"]"), "枚举值没进文档");
    }

    @Test
    @DisplayName("泛型 Result<T> 要用具名 schema 展开，而不是笼统的 Result")
    void genericResultShouldBeExpanded() throws Exception {
        String json = get("/v3/api-docs/01-图书管理");
        assertTrue(json.contains("\"BookResult\""), "单本图书响应没有指向 BookResult");
        assertTrue(json.contains("\"BookPageResult\""), "分页响应没有指向 BookPageResult");
        assertTrue(json.contains("\"PageResultBook\""), "分页泛型没有展开成 PageResultBook");
    }

    @Test
    @DisplayName("校验注解会进文档：pattern / minimum / required 从 @NotBlank、@Pattern 来")
    void validationShouldAppearInSchema() throws Exception {
        String json = get("/v3/api-docs/01-图书管理");
        assertTrue(json.contains("\"pattern\":\"^\\\\d{13}$\""), "ISBN 的正则没进文档");
        assertTrue(json.contains("\"required\":[\"author\",\"isbn\",\"price\",\"stock\",\"title\"]"),
                "必填字段列表不对，检查 @NotBlank / @NotNull");
    }

    @Test
    @DisplayName("练习 4：废弃标记在 JSON 里应该是 deprecated:true")
    void deprecatedFlagShouldBeInJson() throws Exception {
        // 用 JsonNode 按路径取，别用 substring 截字符串：
        // operation 里字段顺序不固定（deprecated 常常排在 responses 之后，能被截断在窗口外），
        // 截断式断言在字段变多时会假失败。
        JsonNode boom = doc("/v3/api-docs/01-图书管理").get("paths").get("/books/boom").get("get");
        assertTrue(boom != null, "找不到 /books/boom");
        assertTrue(boom.path("deprecated").asBoolean(false), "废弃标记没生效，检查 @Operation(deprecated = true)");
    }

    @Test
    @DisplayName("鉴权只挂在写接口上，查询接口不带 security")
    void securityShouldBePerOperation() throws Exception {
        JsonNode paths = doc("/v3/api-docs/01-图书管理").get("paths");

        assertTrue(paths.get("/books").get("post").has("security"), "POST /books 没有标 @SecurityRequirement");
        assertTrue(paths.get("/books/{id}").get("put").has("security"), "PUT /books/{id} 没有标 @SecurityRequirement");
        assertTrue(paths.get("/books/{id}").get("delete").has("security"), "DELETE 没有标 @SecurityRequirement");

        assertTrue(!paths.get("/books").get("get").has("security"), "查询接口不该要求 token（全局 addSecurityItem 会污染所有接口）");
        assertTrue(!paths.get("/books/{id}").get("get").has("security"), "按 id 查询不该要求 token");
    }

    @Test
    @DisplayName("@ParameterObject 断言方法的另一条路：直接调 Controller，绕开 HTTP 看返回结构")
    void pageShouldReturnExpectedShape() {
        BookQuery query = new BookQuery();
        query.setKeyword("Java");
        query.setPageNum(1);
        query.setPageSize(2);

        Result<PageResult<Book>> result = bookController.page(query);
        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().getList().size());
        assertEquals(3, result.getData().getTotal());
    }

    private JsonNode doc(String uri) throws Exception {
        return jsonMapper.readTree(get(uri));
    }

    private String get(String uri) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(uri))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
}
