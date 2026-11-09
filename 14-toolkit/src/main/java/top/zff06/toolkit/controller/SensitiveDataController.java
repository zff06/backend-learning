package top.zff06.toolkit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.toolkit.common.BusinessException;
import top.zff06.toolkit.common.ErrorCode;
import top.zff06.toolkit.common.Result;
import top.zff06.toolkit.dto.CreateCustomerRequest;
import top.zff06.toolkit.dto.CustomerView;
import top.zff06.toolkit.service.SensitiveDataService;

import java.util.Map;

/**
 * 案例 9：敏感数据处理
 *
 * @auther zff06
 * @data 2026/10/22
 **/
@RestController
@RequestMapping("/toolkit/customer")
@RequiredArgsConstructor
public class SensitiveDataController {

    private final SensitiveDataService sensitiveDataService;

    @PostMapping
    public Result<CustomerView> create(@RequestBody CreateCustomerRequest request) {
        return Result.ok(sensitiveDataService.create(request));
    }

    /**
     * role=admin 返回完整信息，其他角色返回脱敏信息。
     * 默认值取 agent：漏传参数时给最保守的结果，而不是明文
     */
    @GetMapping("/{id}")
    public Result<CustomerView> get(@PathVariable String id,
                                    @RequestParam(defaultValue = "agent") String role) {
        return Result.ok(sensitiveDataService.get(id, role));
    }

    /**
     * 查看存储层的密文，演示加密确实生效
     */
    @GetMapping("/{id}/raw")
    public Result<Map<String, Object>> raw(@PathVariable String id) {
        return Result.ok(sensitiveDataService.raw(id));
    }

    @PostMapping("/{id}/verify-password")
    public Result<Boolean> verifyPassword(@PathVariable String id,
                                          @RequestBody PasswordRequest request) {
        if (!sensitiveDataService.verifyPassword(id, request.password())) {
            throw new BusinessException(ErrorCode.PASSWORD_WRONG);
        }
        return Result.ok("密码正确", true);
    }

    public record PasswordRequest(String password) {
    }
}
