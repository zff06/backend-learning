package top.zff06.toolkit.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.zff06.toolkit.common.Result;
import top.zff06.toolkit.dto.ShortLinkResponse;
import top.zff06.toolkit.dto.ShortLinkStats;
import top.zff06.toolkit.service.ShortLinkService;

import java.net.URI;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description ShortLinkController
 **/
@RestController
@RequestMapping("/toolkit")
@RequiredArgsConstructor
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    @PostMapping("/short-link")
    public Result<ShortLinkResponse> create(@Valid @RequestBody CreateRequest request) {
        return Result.ok(shortLinkService.create(request.url(), request.ttlSeconds()));
    }

    /**
     * 访问短链，302 跳到原始地址
     */
    @GetMapping("/s/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        String url = shortLinkService.resolve(code);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }

    @GetMapping("/short-link/{code}")
    public Result<ShortLinkStats> stats(@PathVariable String code) {
        return Result.ok(shortLinkService.stats(code));
    }

    public record CreateRequest(
            @NotBlank(message = "url 不能为空") String url,
            Long ttlSeconds) {
    }
}
