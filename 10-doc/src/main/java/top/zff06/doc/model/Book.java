package top.zff06.doc.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图书实体
 * <p>
 * 实体上的 @Schema：字段说明 + example。example 要写成能直接复制到调试器里就能跑通的值。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description Book
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "图书")
public class Book {

    @Schema(description = "图书 id，自增", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "书名", example = "深入理解 Java 虚拟机", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "作者", example = "周志明")
    private String author;

    @Schema(description = "ISBN（13 位，唯一）", example = "9787111641247")
    private String isbn;

    @Schema(description = "价格，单位元，最多两位小数", example = "129.00")
    private Double price;

    @Schema(description = "库存", example = "20")
    private Integer stock;

    @Schema(description = "图书状态", example = "ON_SALE")
    private BookStatus status;
}
