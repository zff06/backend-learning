package top.zff06.toolkit.dto;

import java.util.List;

/**
 * 敏感词过滤结果
 *
 * @param hits        命中的词
 * @param filtered    替换成星号后的文本
 * @param highlighted 用 mark 标签包起来的文本，前端直接渲染
 * @auther zff06
 * @data 2026/10/22
 */
public record SensitiveResult(List<String> hits, String filtered, String highlighted) {
}
