package top.zff06.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.zff06.security.common.BusinessException;
import top.zff06.security.common.ErrorCode;
import top.zff06.security.dto.ReviewTaskRequest;
import top.zff06.security.dto.SubmitTaskRequest;
import top.zff06.security.entity.Task;
import top.zff06.security.mapper.TaskMapper;
import top.zff06.security.model.TaskRow;
import top.zff06.security.model.TaskStatus;
import top.zff06.security.security.SecurityUser;
import top.zff06.security.vo.TaskView;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务业务：提交、我的提交、详情、全部、批改
 * 归属判断放在服务层：Controller 只收参数，规则不会因为换个入口就被绕过
 * 当前登录人从参数传进来（SecurityUser），服务层不依赖 SecurityContextHolder 静态调用
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;

    /** 学生提交任务：owner 一律取当前登录人，请求体里没有也不信任这个字段 */
    public TaskView submit(SecurityUser user, SubmitTaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setContent(request.getContent());
        task.setOwnerId(user.getId());
        task.setStatus(TaskStatus.SUBMITTED.name());
        task.setSubmittedAt(LocalDateTime.now());
        taskMapper.insert(task);
        return toView(taskMapper.selectByIdWithOwner(task.getId()));
    }

    /** 我的提交：查询条件从令牌取，不从参数取 */
    public List<TaskView> mine(SecurityUser user) {
        return taskMapper.selectByOwnerIdWithOwner(user.getId())
                .stream().map(this::toView).toList();
    }

    /**
     * 任务详情：本人能看，有 task:review 权限的教师和管理员也能看，其他人 403
     */
    public TaskView detail(SecurityUser user, Long id) {
        TaskRow row = taskMapper.selectByIdWithOwner(id);
        if (row == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        boolean isOwner = row.getOwnerId().equals(user.getId());
        boolean canReview = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("task:review"));
        if (!isOwner && !canReview) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return toView(row);
    }

    /** 全部提交：教师与管理员的视角 */
    public List<TaskView> all() {
        return taskMapper.selectAllWithOwner().stream().map(this::toView).toList();
    }

    /**
     * 批改：先查存在、再查状态，已批改的直接抛 30012，别让它更新
     */
    public TaskView review(Long id, ReviewTaskRequest request) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        if (TaskStatus.REVIEWED.name().equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_REVIEWED);
        }
        task.setStatus(TaskStatus.REVIEWED.name());
        task.setScore(request.getScore());
        task.setComment(request.getComment());
        task.setReviewedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        return toView(taskMapper.selectByIdWithOwner(id));
    }

    private TaskView toView(TaskRow row) {
        if (row == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        TaskStatus status = TaskStatus.fromCode(row.getStatus());
        return TaskView.builder()
                .id(row.getId())
                .title(row.getTitle())
                .content(row.getContent())
                .ownerId(row.getOwnerId())
                .owner(row.getOwner())
                .ownerNickname(row.getOwnerNickname())
                .status(row.getStatus())
                .statusText(status == null ? row.getStatus() : status.getText())
                .score(row.getScore())
                .comment(row.getComment())
                .submittedAt(row.getSubmittedAt())
                .reviewedAt(row.getReviewedAt())
                .build();
    }
}
