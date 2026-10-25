package top.zff06.toolkit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.toolkit.common.Result;
import top.zff06.toolkit.dto.SignResponse;
import top.zff06.toolkit.service.SignService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 案例 8：开放接口签名
 *
 * @auther zff06
 * @data 2026/10/22
 **/
@RestController
@RequestMapping("/toolkit/sign")
@RequiredArgsConstructor
public class SignController {

    private final SignService signService;

    /**
     * 生成一个可用的签名，方便直接复制到下面那个接口
     */
    @PostMapping("/demo")
    public Result<SignResponse> demo(@RequestBody(required = false) DemoRequest request) {
        DemoRequest req = request == null ? new DemoRequest(null, null, null, null) : request;
        return Result.ok(signService.demo(req.appId(), req.timestamp(), req.nonce(), req.body()));
    }

    /**
     * 受签名保护的接口。
     * body 用 String 接，不用对象：签名签的是原始字节，反序列化再序列化会改变空格和字段顺序
     */
    @PostMapping("/order")
    public Result<Map<String, Object>> order(
            @RequestHeader(value = "X-App-Id", required = false) String appId,
            @RequestHeader(value = "X-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Nonce", required = false) String nonce,
            @RequestHeader(value = "X-Sign", required = false) String sign,
            @RequestBody(required = false) String body) {

        signService.verify(appId, timestamp, nonce, sign, body);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accepted", true);
        data.put("appId", appId);
        data.put("requestBody", body);
        return Result.ok("签名校验通过", data);
    }

    public record DemoRequest(String appId, Long timestamp, String nonce, String body) {
    }
}
