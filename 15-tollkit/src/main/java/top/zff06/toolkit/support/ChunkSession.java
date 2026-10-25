package top.zff06.toolkit.support;

import lombok.Getter;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description ChunkSession
 **/
@Getter
public class ChunkSession {

    private final String uploadId;
    private final String fileName;
    private final int totalChunks;
    private final long chunkSize;
    private final String md5;
    private final long createdAt = System.currentTimeMillis();

    public ChunkSession(String uploadId, String fileName, int totalChunks,
                        long chunkSize, String md5) {
        this.uploadId = uploadId;
        this.fileName = fileName;
        this.totalChunks = totalChunks;
        this.chunkSize = chunkSize;
        this.md5 = md5;
    }
}
