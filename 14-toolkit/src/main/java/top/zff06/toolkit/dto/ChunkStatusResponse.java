package top.zff06.toolkit.dto;

import java.util.List;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description ChunkStatusResponse
 **/
public record ChunkStatusResponse(String uploadId, int totalChunks,
                                  List<Integer> uploaded, List<Integer> missing) {
}
