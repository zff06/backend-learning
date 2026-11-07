package top.zff06.test.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.test.common.Result;
import top.zff06.test.dto.OrderRequest;
import top.zff06.test.dto.OrderResult;
import top.zff06.test.service.OrderService;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 订单接口：三个入口，只有 calculate 带 @Valid。
 *
 * <p>quick-calculate 走查询参数，拼成 OrderRequest 直接传给 Service，校验注解在这里不生效——
 * 所以金额传 0 会由 Service 抛业务异常，返回 400 + 业务码 30400 而不是校验失败的 400。
 * 这个不一致是刻意留的，它是"Service 层兜底校验"的活例子。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description OrderController
 **/
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/calculate")
    public Result<OrderResult> calculate(@Valid @RequestBody OrderRequest request) {
        return Result.ok(orderService.calculate(request));
    }

    @GetMapping("/quick-calculate")
    public Result<OrderResult> quickCalculate(@RequestParam BigDecimal amount,
                                              @RequestParam(defaultValue = "NORMAL") String memberLevel) {
        return Result.ok(orderService.calculate(new OrderRequest(1L, amount, memberLevel, 1)));
    }

    @GetMapping("/hello")
    public Result<Map<String, Object>> hello() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("module", "11-test");
        data.put("hint", "执行 mvn -pl 11-test test 可以跑全部测试用例");
        data.put("coverage", "target/site/jacoco/index.html");
        return Result.ok(data);
    }
}
