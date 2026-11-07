package top.zff06.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import top.zff06.file.service.OssService;

import java.util.Arrays;
import java.util.List;

/**
 * @auther zff06
 * @data 2026/11/4
 * @description OssController
 **/
@Tag(name = "文件上传", description = "阿里云 OSS 单图 / 多图上传")
@RestController
@RequestMapping("/oss")
@RequiredArgsConstructor
public class OssController {
    private final OssService ossService;

    /**
     * 练习 1 的问题一：MultipartFile 只写 @RequestParam 时，文档里是一个没有说明的对象。
     * 修法：显式声明 consumes = multipart/form-data，并用 @Parameter(content = @Content(mediaType = OCTET_STREAM))
     *      （或改用 @RequestPart）告诉 springdoc 这是二进制文件字段，UI 才会给"选择文件"按钮。
     * 练习 1 的问题二在 uploadBatch 上：返回 List&lt;String&gt; 时 UI 里只剩笼统 schema，
     *      用 @ArraySchema 显式声明数组元素类型。
     */
    @Operation(summary = "单图上传", description = "表单字段名 file，返回图片访问 URL")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "上传成功，返回图片 URL",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(type = "string",
                                    example = "https://zff06-bucket.oss-cn-hangzhou.aliyuncs.com/2026/11/07/demo.png"))),
            @ApiResponse(responseCode = "400", description = "文件为空或超出大小限制",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(type = "string", example = "上传失败：文件大小超出限制")))
    })
    @PostMapping(value = "upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(
            @Parameter(description = "要上传的图片，单文件上限 5MB", required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestPart("file") MultipartFile file) {
        return ossService.upload(file);
    }

    /**
     * 多图上传：前端表单里多个 input 均用 name="files"，或一个 input 开启 multiple
     */
    @Operation(summary = "多图上传", description = "表单字段名 files，可重复；返回 URL 列表，顺序与入参一致")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "上传成功，返回 URL 数组",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string",
                                    example = "https://zff06-bucket.oss-cn-hangzhou.aliyuncs.com/2026/11/07/demo.png")))),
            @ApiResponse(responseCode = "400", description = "文件为空或超出大小限制",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(type = "string", example = "上传失败：文件大小超出限制")))
    })
    @PostMapping(value = "uploadBatch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<String> uploadBatch(
            @Parameter(description = "要上传的图片列表，单个上限 5MB，合计上限 20MB", required = true,
                    array = @ArraySchema(schema = @Schema(type = "string", format = "binary")))
            @RequestPart("files") MultipartFile[] files) {
        return ossService.uploadBatch(Arrays.asList(files));
    }

    /**
     * 文件大小超出 application.yml 里配置的上限时，返回友好提示而不是 500 错误页
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleSizeLimit(MaxUploadSizeExceededException e) {
        return "上传失败：文件大小超出限制";
    }
}
