package top.zff06.security.model;

import lombok.Getter;

/**
 * 任务状态：只有两个值，中文文案由后端给出，前端直接显示不再映射
 */
@Getter
public enum TaskStatus {

    SUBMITTED("待批改"),
    REVIEWED("已批改");

    private final String text;

    TaskStatus(String text) {
        this.text = text;
    }

    /** 按状态编码查枚举 */
    public static TaskStatus fromCode(String code) {
        for (TaskStatus status : values()) {
            if (status.name().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
