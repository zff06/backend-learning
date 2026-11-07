# 模块 10 · 接口文档综合练习

教程：<https://www.workbuddy.cn/space/d/I6jdbrghltbh2G5pEjMyGo>（模块 10 · 接口文档）

技术栈：Spring Boot 4.1.1 / JDK 21 / springdoc 3.2.0。Boot 4 必须用 springdoc 3.x，2.x 只支持 Boot 3。

## 跑起来

```bash
# 依赖（首次）
"D:/Tools/apache-maven-3.9.16/bin/mvn.cmd" -B -pl 10-doc compile

# 起服务
env -u SERVER__PORT -u SERVER__HOST "D:/Tools/apache-maven-3.9.16/bin/mvn.cmd" -pl 10-doc spring-boot:run

# 测试（18 个用例）
env -u SERVER__PORT -u SERVER__HOST "D:/Tools/apache-maven-3.9.16/bin/mvn.cmd" -pl 10-doc test
```

入口：

| 地址 | 内容 |
| --- | --- |
| `http://localhost:8010/swagger-ui.html` | Swagger UI，右上角有 Authorize |
| `http://localhost:8010/v3/api-docs` | 全量 OpenAPI JSON |
| `http://localhost:8010/v3/api-docs/01-图书管理` | 按路径切的分组 |
| `http://localhost:8010/v3/api-docs/02-系统运维` | 按包切的分组 |
| `http://localhost:8010/v3/api-docs/swagger-config` | 分组清单 + UI 排序/展开配置 |

---

## 练习 1：给已有模块补文档（09-file）

只加了 springdoc 依赖 + 注解，OSS 业务代码没动。运行后 UI 缺的东西正好是教程说的两条：

**问题一：`MultipartFile` 显示成一个没说明的对象。**
原写法 `upload(@RequestParam("file") MultipartFile file)` 在文档里是个裸对象，UI 没有"选择文件"按钮。

修法（两条都做）：

```java
@PostMapping(value = "upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public String upload(
        @Parameter(description = "要上传的图片，单文件上限 5MB", required = true,
                content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                        schema = @Schema(type = "string", format = "binary")))
        @RequestPart("file") MultipartFile file) {
```

生成的 JSON 变成：

```json
"requestBody": {
  "content": {
    "multipart/form-data": {
      "schema": { "type": "object", "required": ["file"],
        "properties": { "file": { "type": "string", "format": "binary",
                                  "description": "要上传的图片，单文件上限 5MB" } } },
      "encoding": { "file": { "contentType": "application/octet-stream" } }
    }
  }
}
```

关键点：`consumes = multipart/form-data` 决定 requestBody 挂在哪个 media type 下；`format = binary` 才是 UI 给文件选择框的依据。`@RequestPart` 与 `@RequestParam` 对 multipart 绑定效果一致（实测 `POST /oss/upload` 仍然正常进业务逻辑并命中扩展名校验）。

**问题二：`Result<T>` / `List<String>` 的泛型在文档里退化成笼统 schema。**

`Result` 是泛型，方法签名上 springdoc 只看得见 `Result`，猜不出 `T`。09-file 返回的是裸 `List<String>`，UI 里只剩一个 `array` 没有元素类型。修法是显式声明：

```java
@ApiResponse(responseCode = "200", description = "上传成功，返回 URL 数组",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(type = "string", example = "https://.../demo.png"))))
```

10-doc 里 `Result<Book>` 这种走的是另一条路——给 `Result<X>` 组合定义空壳子类（`schema/DocSchemas.java`），再 `schema = @Schema(implementation = BookResult.class)`：

```json
"BookResult":  { "code": ..., "message": ..., "data": { "$ref": "#/components/schemas/Book" } }
"BookPageResult": { ..., "data": { "$ref": "#/components/schemas/PageResultBook" } }
```

---

## 练习 2：加鉴权配置

`config/OpenApiConfig.java` 登记安全方案：

```java
.components(new Components().addSecuritySchemes("bearer",
        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
```

在写接口上标 `@SecurityRequirement(name = "bearer")`（新增 / 修改 / 删除 + `/system/health`）。

**一个刻意的取舍**：没有 `addSecurityItem()` 做全局声明。加了全局声明后，`GET /books`、`GET /books/{id}` 这些公开接口在 JSON 里也会带 `security`，用 openapi 生成客户端的那一方会要求处处传 token。鉴权按 operation 粒度标，`ApiDocTest#securityShouldBePerOperation` 专门断言了"写接口有、查询接口没有"。

验证：UI 右上角出现 Authorize（`persist-authorization: true`，刷新不丢 token）。

---

## 练习 3：把文档校验接进 CI

两种接法，仓库里都给了：

**A. 纯测试版（推荐）** —— `src/test/java/top/zff06/doc/OpenApiContractTest.java`

CI 里只要 `mvn -pl 10-doc test` 就会跑。逻辑：

1. MockMvc 直接取 `/v3/api-docs`（测试进程里拿文档，省掉"先起服务"这一步，也不会因为 8010 被自己的 IDEA 实例占着而连错进程）
2. 递归 key 排序 → 固定缩进文本
3. 与 `src/test/resources/openapi-baseline.json` 逐行 diff
4. 有差异就失败，报 `接口契约有变更，请确认是否需要通知调用方` + 前 40 行差异

基线更新：`mvn -pl 10-doc test -Ddoc.baseline.update=true`，然后 review 并一起提交。

**B. 独立脚本版** —— `ci/openapi-diff.sh`（真起服务、导出、diff，顺带验证文档端点真的开着）

```bash
bash 10-doc/ci/openapi-diff.sh          # 退出码 0=未变 1=有变更 2=起不来
```

**排序是必须的**：path / schema / property 都是 Map，迭代顺序不受控，加一个字段就可能让后面所有行整体错位。另外脚本还剔掉了 `servers`——它的 url 是"实例地址+端口"，换端口就变，不属于接口契约（第一次跑就被这条误报过）。

---

## 练习 4：给接口加废弃标记

```java
@Operation(summary = "【已废弃】触发一次 500", deprecated = true, description = "改调用 /books/{id}")
@GetMapping("/boom")
```

两处变化：UI 里标题带删除线 + `DEPRECATED` 灰标；JSON 里该 operation 多出 `"deprecated": true`。

**踩到的坑**：一开始用 `json.substring(idx, idx + 400)` 截字符串断言，结果失败了。原因是 operation 里字段顺序不固定，`deprecated` 排在 `responses` 之后，被截在窗口外——JSON 里其实有。改成 `JsonNode.path("deprecated").asBoolean(false)` 按路径取就好。

---

## 练习 5：限制文档的访问

选了**方案一：`springdoc` 开关**（`src/main/resources/application-prod.yml`）：

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

验证：`ProdProfileDocDisabledTest` 用 `@ActiveProfiles("prod")` 断言 `/v3/api-docs` 与 `/swagger-ui.html` 都不可用、`/books/1` 照常。

**断言口径要注意**：本模块有 `GlobalExceptionHandler` 接 `NoResourceFoundException`，HTTP 状态还是 200，真正的信号是响应体 `{"code":404,"message":"资源不存在"}`。只写 `status().isNotFound()` 会误判成"文档没关掉"。

### 为什么选它

- 生产环境关文档，目的是"别把内部接口地图挂公网"。开关方式零依赖、零额外攻击面，也不影响业务代码。
- 方案二（Spring Security 拦 `/v3/api-docs/**`）适合"内网也要用文档"的场景，但给一个学习模块引进整套安全链，收益小于成本；而且 08-security 已经演示过这套，不必在这里重复。
- 方案三（单独部署文档服务）是正确的最终形态（文档跟着内网域名、与业务隔离），代价是要多一套部署管道，对当前阶段过重。

### 为什么生产环境不建议公开文档

1. **暴露攻击面清单**：文档把所有路径、参数、错误码一次列全，省了攻击者到处 fuzz。
2. **泄露内部实现**：schema 里的字段名往往就是表结构；`@Schema(example)` 如果照抄真实数据（手机号、内部 id 规则）会直接外泄。
3. **调试入口变成后门**：`/books/boom` 这类演示接口、`/system/health` 这类内部探针，生产环境不该公网可达。
4. **契约漂移没人管**：文档挂公网就当成了对外承诺，接口改一次就要通知一轮调用方，反而拖慢迭代。
5. **合规**：等保 / 渗透测试基本都会把"未授权访问的 Swagger UI"列成中危项。

配套约定：开发环境开，生产关；需要给外部看的接口另外出一份裁剪过的 OpenAPI（只保留稳定接口），走 API 网关发布。

---

## 教程里最容易漏的四条

| 坑 | 症状 | 修法 |
| --- | --- | --- |
| 查询对象没加 `@ParameterObject` | UI 里只有一个 `bookQuery` 空对象框 | 参数上加 `@ParameterObject`，JSON 里变成 4 个独立 `in: query` |
| 用了 Spring 的 `@RequestBody` | 请求体没有示例 | 换成 `io.swagger.v3.oas.annotations.parameters.RequestBody`（同名类，import 写错不报错） |
| 枚举没加 `@Schema(description)` | UI 里只有 `ON_SALE` 一串裸值 | 枚举值和字段都标 `@Schema`，JSON 里至少带上 `enum` + description |
| `pathsToMatch` 写错（`/book/**` 少个 s） | 分组文档空白，不报错 | 分组要单独写测试，`ApiDocTest#groupsShouldBeSeparated` 就是干这个的 |

## 本节遵循的规范

1. 每个接口必须有 `@Operation(summary=...)`
2. 查询对象必须加 `@ParameterObject`
3. 响应码用 `@ApiResponse` 列全（含业务错误码，给 `@ExampleObject`）
4. 文档里的 example 要能直接复制进调试器跑通
5. 生产环境不公开 `api-docs`
