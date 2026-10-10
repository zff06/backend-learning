package top.zff06.web.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.web.common.PageResult;
import top.zff06.web.common.Result;
import top.zff06.web.dto.UserCreateRequest;
import top.zff06.web.dto.UserUpdateRequest;
import top.zff06.web.entity.User;
import top.zff06.web.service.UserService;

/**
 * 用户接口
 *
 * @author mqxu
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result<PageResult<User>> page(@RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNum 不能小于 1") int pageNum,
                                         @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize 不能小于 1") @Max(value = 100, message = "pageSize 不能大于 100") int pageSize) {
        return Result.ok(userService.page(pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<User> get(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    @PostMapping
    public Result<User> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.ok(userService.create(request));
    }

    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return Result.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }
}
