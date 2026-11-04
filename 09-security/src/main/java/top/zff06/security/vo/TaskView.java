package top.zff06.security.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务视图：statusText 由后端给出中文，前端直接显示
 */
@Data
@Builder
public class TaskView {

    private Long id;

    private String title;

    private String content;

    private Long ownerId;

    /** 提交人用户名 */
    private String owner;

    private String ownerNickname;

    /** SUBMITTED / REVIEWED，逻辑判断用这个 */
    private String status;

    /** 中文状态文案 */
    private String statusText;

    /** 未批改时为 null */
    private Integer score;

    /** 未批改时为 null */
    private String comment;

    private LocalDateTime submittedAt;

    /** 未批改时为 null */
    private LocalDateTime reviewedAt;
}
