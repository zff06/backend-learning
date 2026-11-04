package top.zff06.file.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @auther zff06
 * @data 2026/11/4
 * @description OssService
 **/
public interface OssService{
    String upload(MultipartFile file);

    /**
     * 多图上传
     *
     * @param files 文件列表
     * @return 上传后的访问 URL 列表（顺序与入参一致）
     */
    List<String> uploadBatch(List<MultipartFile> files);
}
