package top.zff06.toolkit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.zff06.toolkit.common.Result;
import top.zff06.toolkit.dto.ImportResult;
import top.zff06.toolkit.service.ExcelService;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description ExcelController
 **/
@RestController
@RequestMapping("/toolkit/excel")
@RequiredArgsConstructor
public class ExcelController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExcelService excelService;

    /**
     * 导出订单报表
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        return attachment(excelService.export(), "订单报表.xlsx");
    }

    /**
     * 下载导入模板，状态列带下拉框
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        return attachment(excelService.template(), "订单导入模板.xlsx");
    }

    /**
     * 导入并校验，有错误时返回错误明细的下载地址
     */
    @PostMapping("/import")
    public Result<ImportResult> importFile(@RequestParam MultipartFile file) {
        return Result.ok(excelService.importFile(file));
    }

    /**
     * 下载错误明细
     */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<FileSystemResource> download(@PathVariable String fileName) {
        File file = excelService.errorFile(fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachmentHeader("导入错误明细.xlsx"))
                .contentType(XLSX)
                .body(new FileSystemResource(file));
    }

    private ResponseEntity<byte[]> attachment(byte[] bytes, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachmentHeader(fileName))
                .contentType(XLSX)
                .body(bytes);
    }

    private String attachmentHeader(String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }
}
