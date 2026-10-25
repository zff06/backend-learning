package top.zff06.toolkit.dto;

/**
 * 文件检查结果
 *
 * @param originalName 原始文件名
 * @param extension    扩展名
 * @param realType     真实类型（按文件头识别）
 * @param size         字节数
 * @param md5          MD5
 * @param allowed      是否在白名单内
 * @param remark       扩展名与真实类型是否一致
 * @auther zff06
 * @data 2026/10/22
 */
public record FileCheckResult(String originalName, String extension, String realType,
                              long size, String md5, boolean allowed, String remark) {
}
