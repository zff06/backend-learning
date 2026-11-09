package top.zff06.toolkit.dto;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description ChunkMergeResponse
 **/
public record ChunkMergeResponse(String fileId, String fileName, long size,
                                 String md5, String downloadUrl) {
}
