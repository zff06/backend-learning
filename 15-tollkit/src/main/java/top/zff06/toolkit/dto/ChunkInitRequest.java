package top.zff06.toolkit.dto;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description ChunkInitRequest
 **/
public record ChunkInitRequest(String fileName, Integer totalChunks, String md5) {
}
