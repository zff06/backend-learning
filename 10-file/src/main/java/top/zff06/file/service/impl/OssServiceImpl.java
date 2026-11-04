package top.zff06.file.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.zff06.file.config.OssConfig;
import top.zff06.file.service.OssService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * @auther zff06
 * @data 2026/11/4
 * @description OssServiceImpl
 **/
@Service
@Slf4j
public class OssServiceImpl implements OssService {

    /**
     * 允许上传的图片后缀白名单
     */
    private static final Set<String> ALLOWED_SUFFIXES = Set.of(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");

    @Resource
    private OssConfig ossConfig;

    @Override
    public String upload(MultipartFile file) {
        if (file != null) {
            // 拦截非图片类型的文件
            checkImage(file);
            // 获取原始文件名
            String originalFilename = file.getOriginalFilename();
            // 获取文件后缀
            assert originalFilename != null;
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            // 生成新的文件名
            String newFileName = UUID.randomUUID() + suffix;
            log.info("新文件名: {}", newFileName);
            // 读取配置文件中的配置
            String endpoint = ossConfig.getEndpoint();
            String bucket = ossConfig.getBucket();
            String accessKey = ossConfig.getAccessKey();
            String secretKey = ossConfig.getSecretKey();
            String dir = ossConfig.getDir();
            // 创建OSS客户端
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
            // 创建ObjectMetadata对象，设置上传文件对象的元数据
            ObjectMetadata meta = new ObjectMetadata();
            // 元数据使用文件真实的 Content-Type，浏览器才能正确预览 png/gif 等格式
            meta.setContentType(file.getContentType());
            // 指定上传文件的路径和名称，例如 img/xxx.jpg
            String uploadPath = dir + newFileName;
            InputStream inputStream;
            try {
                // 获取文件输入流
                inputStream = file.getInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // 使用putObject方法上传，参数为存储空间名称、文件名称、文件输入流，以及文件元信息
            ossClient.putObject(bucket, uploadPath, inputStream, meta);
            // 关闭OSS客户端
            ossClient.shutdown();
            log.info("文件上传后的访问地址是:https://{}.{}/{}", bucket, endpoint, uploadPath);
            // 返回文件访问路径
            return "https://" + bucket + "." + endpoint + "/" + uploadPath;
        }
        return "上传失败";
    }

    @Override
    public List<String> uploadBatch(List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        if (files != null) {
            // 先整体校验，任何一个不是图片都不上传，避免“传了一半”
            for (MultipartFile file : files) {
                checkImage(file);
            }
            // 校验全部通过后，逐个复用单图上传逻辑
            for (MultipartFile file : files) {
                urls.add(upload(file));
            }
        }
        return urls;
    }

    /**
     * 校验文件是否为图片：后缀在白名单内，且 Content-Type 以 image/ 开头。
     * 两层校验缺一不可——只看 Content-Type 可被伪造，只看后缀挡不住改名的可执行文件。
     */
    private void checkImage(MultipartFile file) {
        // 文件为空直接拦截
        if (file.isEmpty()) {
            throw new IllegalArgumentException("存在空文件，已拦截");
        }
        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        // 校验文件后缀
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("文件 [" + originalFilename + "] 缺少扩展名，已拦截");
        }
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        if (!ALLOWED_SUFFIXES.contains(suffix)) {
            throw new IllegalArgumentException("文件 [" + originalFilename + "] 的扩展名 " + suffix + " 不是允许的图片类型，已拦截");
        }
        // 校验 Content-Type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("文件 [" + originalFilename + "] 不是图片类型，已拦截");
        }
    }
}
