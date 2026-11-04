package top.zff06.security.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务查询投影：任务字段 + 提交人的用户名与姓名
 * 联表查出来的结果，不让实体直接出 service 层
 */
@Data
public class TaskRow {

    private Long id;

    private String title;

    private String content;

    private Long ownerId;

    /** 提交人用户名，如 student01 */
    private String owner;

    private String ownerNickname;

    private String status;

    private Integer score;

    private String comment;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;
}
