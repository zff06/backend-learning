package top.zff06.toolkit.dto;

import java.util.List;

/**
 * 巡检请求
 *
 * @param urls 待探测的地址
 * @auther zff06
 * @data 2026/10/22
 */
public record InspectRequest(List<String> urls) {
}
