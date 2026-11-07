package top.zff06.doc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 分页响应
 *
 * @auther zff06
 * @data 2026/11/7
 * @description PageResult
 **/
@Data
@AllArgsConstructor
@Schema(description = "分页结果")
public class PageResult<T> {

    @Schema(description = "当前页数据")
    private List<T> list;

    @Schema(description = "总条数", example = "3")
    private long total;

    @Schema(description = "页码", example = "1")
    private int pageNum;

    @Schema(description = "每页条数", example = "10")
    private int pageSize;
}
