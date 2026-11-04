package top.zff06.security.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.security.common.Result;
import top.zff06.security.dto.ReviewTaskRequest;
import top.zff06.security.dto.SubmitTaskRequest;
import top.zff06.security.security.SecurityUser;
import top.zff06.security.service.TaskService;
import top.zff06.security.vo.TaskView;

import java.util.List;

/**
 * 任务接口：绑权限点，不绑角色
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasAuthority('task:submit')")
    public Result<TaskView> submit(@AuthenticationPrincipal SecurityUser user,
                                   @Valid @RequestBody SubmitTaskRequest request) {
        return Result.ok(taskService.submit(user, request));
    }

    @GetMapping("/mine")
    public Result<List<TaskView>> mine(@AuthenticationPrincipal SecurityUser user) {
        return Result.ok(taskService.mine(user));
    }

    @GetMapping("/{id}")
    public Result<TaskView> detail(@AuthenticationPrincipal SecurityUser user,
                                   @PathVariable Long id) {
        return Result.ok(taskService.detail(user, id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('task:review')")
    public Result<List<TaskView>> all() {
        return Result.ok(taskService.all());
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('task:review')")
    public Result<TaskView> review(@PathVariable Long id,
                                   @Valid @RequestBody ReviewTaskRequest request) {
        return Result.ok(taskService.review(id, request));
    }
}
