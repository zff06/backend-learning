package top.zff06.toolkit.dto;

import java.util.List;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description ChunkInitResponse
 **/
public record ChunkInitResponse(String uploadId, long chunkSize,
                                int totalChunks, List<Integer> uploaded) {
}
