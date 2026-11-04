package top.zff06.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交任务请求：归属不由请求体决定，服务端从令牌写入
 */
@Data
public class SubmitTaskRequest {

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 100, message = "任务标题最长 100 字")
    private String title;

    @NotBlank(message = "完成情况不能为空")
    @Size(max = 2000, message = "完成情况最长 2000 字")
    private String content;
}
