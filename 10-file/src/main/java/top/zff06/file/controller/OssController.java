package top.zff06.file.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import top.zff06.file.service.OssService;

import java.util.Arrays;
import java.util.List;

/**
 * @auther zff06
 * @data 2026/11/4
 * @description OssController
 **/
@RestController
@RequestMapping("/oss")
@RequiredArgsConstructor
public class OssController {
    private final OssService ossService;

    @PostMapping("upload")
    public String upload(@RequestParam("file") MultipartFile file){
        return ossService.upload(file);
    }

    /**
     * 多图上传：前端表单里多个 input 均用 name="files"，或一个 input 开启 multiple
     */
    @PostMapping("uploadBatch")
    public List<String> uploadBatch(@RequestParam("files") MultipartFile[] files){
        return ossService.uploadBatch(Arrays.asList(files));
    }

    /**
     * 文件大小超出 application.yml 里配置的上限时，返回友好提示而不是 500 错误页
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleSizeLimit(MaxUploadSizeExceededException e) {
        return "上传失败：文件大小超出限制";
    }
}
