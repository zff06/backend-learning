package top.zff06.doc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.zff06.doc.model.BookStatus;

/**
 * 图书分页查询参数
 * <p>
 * 这是"查询对象"：Controller 里如果不加 @ParameterObject，
 * springdoc 会把这个对象当成一个名叫 bookQuery 的复合参数，页面上只有一个空对象框，无法单独填 keyword/status。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description BookQuery
 **/
@Data
@Schema(description = "图书分页查询参数")
public class BookQuery {

    @Schema(description = "书名 / 作者关键字，模糊匹配", example = "Java")
    private String keyword;

    @Schema(description = "图书状态，不传表示全部", example = "ON_SALE")
    private BookStatus status;

    @Schema(description = "页码，从 1 开始", defaultValue = "1", example = "1", minimum = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，1~100", defaultValue = "10", example = "10", minimum = "1", maximum = "100")
    private Integer pageSize = 10;
}
