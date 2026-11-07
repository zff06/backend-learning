package top.zff06.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.annotations.ParameterObject;
import top.zff06.doc.common.Result;
import top.zff06.doc.dto.BookQuery;
import top.zff06.doc.dto.BookSaveRequest;
import top.zff06.doc.model.Book;
import top.zff06.doc.schema.DocSchemas.BookPageResult;
import top.zff06.doc.schema.DocSchemas.BookResult;
import top.zff06.doc.schema.DocSchemas.VoidResult;
import top.zff06.doc.service.BookService;
import top.zff06.doc.vo.PageResult;

/**
 * 图书接口
 * <p>
 * 控制器上的三个注解：@Tag（分组标题）、@Operation（接口一句话说明）、@ApiResponse（响应码列全）。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description BookController
 **/
@Tag(name = "图书管理", description = "图书的增删改查，供后续订单模块引用")
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @Operation(summary = "分页查询图书",
            description = "按书名 / 作者关键字模糊匹配，可按状态过滤。注意：查询对象必须加 @ParameterObject，否则页面上只显示一个 bookQuery 复合参数。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BookPageResult.class)))
    })
    @GetMapping
    public Result<PageResult<Book>> page(@ParameterObject BookQuery query) {
        return Result.ok(bookService.page(query));
    }

    @Operation(summary = "按 id 查询图书")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookResult.class))),
            @ApiResponse(responseCode = "400", description = "图书不存在（code=30001）",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VoidResult.class),
                            examples = @ExampleObject(name = "图书不存在", value = """
                                    {"code":30001,"message":"图书不存在","data":null}
                                    """)))
    })
    @GetMapping("/{id}")
    public Result<Book> get(@Parameter(description = "图书 id", example = "1", required = true,
            in = ParameterIn.PATH) @PathVariable Long id) {
        return Result.ok(bookService.get(id));
    }

    @Operation(summary = "新增图书", description = "ISBN 全局唯一，重复返回 code=30002")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "新增成功",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookResult.class))),
            @ApiResponse(responseCode = "400", description = "参数校验失败 / ISBN 重复",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VoidResult.class),
                            examples = @ExampleObject(name = "ISBN 重复", value = """
                                    {"code":30002,"message":"ISBN 已存在","data":null}
                                    """)))
    })
    @SecurityRequirement(name = "bearer")
    @PostMapping
    public Result<Book> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "图书信息，注意这里是 swagger 的 @RequestBody，不是 Spring 的那个",
                    required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookSaveRequest.class),
                            examples = @ExampleObject(name = "新增一本 Java 书", value = """
                                    {"title":"Java 核心技术","author":"Cay S. Horstmann","isbn":"9787111636663","price":149.00,"stock":30,"status":"ON_SALE"}
                                    """)))
            @RequestBody @jakarta.validation.Valid BookSaveRequest request) {
        return Result.ok(bookService.create(request));
    }

    @Operation(summary = "修改图书", description = "全量覆盖，id 不存在返回 code=30001")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修改成功",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookResult.class))),
            @ApiResponse(responseCode = "400", description = "图书不存在 / 参数校验失败",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VoidResult.class)))
    })
    @SecurityRequirement(name = "bearer")
    @PutMapping("/{id}")
    public Result<Book> update(@Parameter(description = "图书 id", example = "1", required = true,
            in = ParameterIn.PATH) @PathVariable Long id,
                               @RequestBody @jakarta.validation.Valid BookSaveRequest request) {
        return Result.ok(bookService.update(id, request));
    }

    @Operation(summary = "删除图书", description = "物理删除，id 不存在返回 code=30001")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VoidResult.class))),
            @ApiResponse(responseCode = "400", description = "图书不存在",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VoidResult.class)))
    })
    @SecurityRequirement(name = "bearer")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "图书 id", example = "1", required = true,
            in = ParameterIn.PATH) @PathVariable Long id) {
        bookService.delete(id);
        return Result.ok();
    }

    /**
     * 练习 4：@Operation(deprecated = true)
     * <p>
     * 效果有两处：Swagger UI 里该接口标题被打上删除线 + 灰色 DEPRECATED 标签；
     * 导出的 openapi.json 中该 operation 上出现 "deprecated": true。
     */
    @Operation(summary = "【已废弃】触发一次 500", deprecated = true,
            description = "仅用于演示 5xx 响应在文档里的呈现。已废弃，不要在新代码里调用，改调用 /books/{id}。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "正常情况下不会走到这里",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VoidResult.class))),
            @ApiResponse(responseCode = "500", description = "服务端异常（code=50000）",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VoidResult.class),
                            examples = @ExampleObject(name = "系统异常", value = """
                                    {"code":500,"message":"系统繁忙，请稍后再试","data":null}
                                    """)))
    })
    @GetMapping("/boom")
    public Result<Void> boom() {
        bookService.boom();
        return Result.ok();
    }
}
