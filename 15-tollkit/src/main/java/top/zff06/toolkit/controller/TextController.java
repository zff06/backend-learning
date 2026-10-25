package top.zff06.toolkit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.toolkit.common.Result;
import top.zff06.toolkit.dto.SensitiveResult;
import top.zff06.toolkit.dto.TextExtractResult;
import top.zff06.toolkit.service.TextService;

/**
 * 案例 12：文本清洗
 *
 * @auther zff06
 * @data 2026/10/22
 **/
@RestController
@RequestMapping("/toolkit/text")
@RequiredArgsConstructor
public class TextController {

    private final TextService textService;

    /**
     * 提取并脱敏
     */
    @PostMapping("/extract")
    public Result<TextExtractResult> extract(@RequestBody TextRequest request) {
        return Result.ok(textService.extract(request.text()));
    }

    /**
     * 敏感词过滤
     */
    @PostMapping("/sensitive")
    public Result<SensitiveResult> sensitive(@RequestBody TextRequest request) {
        return Result.ok(textService.filter(request.text()));
    }

    /**
     * 文本为空时返回空结果比返回 400 更合理，所以不加 @Valid
     */
    public record TextRequest(String text) {
    }
}
