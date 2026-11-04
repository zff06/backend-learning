package top.zff06.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务实体，对应 sec_task 表
 */
@Data
@TableName("sec_task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /** 提交人，指向 sec_user.id，归属由服务端从令牌写入，不信任请求体 */
    private Long ownerId;

    /** SUBMITTED 待批改 / REVIEWED 已批改 */
    private String status;

    private Integer score;

    private String comment;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;
}
