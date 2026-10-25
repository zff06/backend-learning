package top.zff06.toolkit.dto;

import java.util.List;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description ImportResult
 **/
public record ImportResult(int total, int success, int failed,
                           List<RowError> errors, String errorFileUrl) {

    public record RowError(int row, String orderNo, String message) {
    }
}
