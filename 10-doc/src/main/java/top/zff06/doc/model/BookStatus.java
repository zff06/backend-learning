package top.zff06.doc.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 图书状态
 * <p>
 * 枚举值上的 @Schema(description) 会把"裸值 ON_SALE"变成"ON_SALE —— 在售"，
 * 少了它就是 UI 里一串看不懂的大写常量。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description BookStatus
 **/
@Getter
@Schema(description = "图书状态")
public enum BookStatus {

    @Schema(description = "在售")
    ON_SALE("在售"),

    @Schema(description = "已下架")
    OFF_SHELF("已下架"),

    @Schema(description = "已售罄")
    SOLD_OUT("已售罄");

    private final String label;

    BookStatus(String label) {
        this.label = label;
    }
}
