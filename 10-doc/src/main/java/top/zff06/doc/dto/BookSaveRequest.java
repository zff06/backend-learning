package top.zff06.doc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import top.zff06.doc.model.BookStatus;

import java.math.BigDecimal;

/**
 * 新增 / 修改图书请求体
 * <p>
 * 字段上的 @NotBlank、@Min、@Pattern 会被 springdoc 读进文档：
 * required 列表、minLength/maxLength、minimum/maximum、pattern 都会出现在 schema 里。
 * 练习的重点之一就是"校验注解不用另写一遍文档"。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description BookSaveRequest
 **/
@Data
@Schema(description = "新增 / 修改图书请求体")
public class BookSaveRequest {

    @NotBlank(message = "书名不能为空")
    @Size(max = 50, message = "书名最多 50 个字")
    @Schema(description = "书名", example = "深入理解 Java 虚拟机", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "作者不能为空")
    @Size(max = 20, message = "作者最多 20 个字")
    @Schema(description = "作者", example = "周志明", requiredMode = Schema.RequiredMode.REQUIRED)
    private String author;

    @NotBlank(message = "ISBN 不能为空")
    @Pattern(regexp = "^\\d{13}$", message = "ISBN 必须是 13 位数字")
    @Schema(description = "ISBN，13 位数字，全局唯一", example = "9787111641247",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String isbn;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    @Schema(description = "价格，单位元", example = "129.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    @Max(value = 99999, message = "库存超出上限")
    @Schema(description = "库存", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer stock;

    @Schema(description = "图书状态，不传默认 ON_SALE", example = "ON_SALE")
    private BookStatus status;
}
