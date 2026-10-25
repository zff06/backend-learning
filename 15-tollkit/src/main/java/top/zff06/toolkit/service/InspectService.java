package top.zff06.toolkit.service;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.zff06.toolkit.common.BusinessException;
import top.zff06.toolkit.common.ErrorCode;
import top.zff06.toolkit.config.ToolkitProperties;
import top.zff06.toolkit.dto.InspectResult;
import top.zff06.toolkit.dto.InspectSummary;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * 接口巡检
 *
 * @auther zff06
 * @data 2026/10/22
 * @description 线程池并发探测，每个地址一个任务，最后按耗时排序汇总
 **/
@Service
@RequiredArgsConstructor
public class InspectService {

    private final ToolkitProperties properties;

    public InspectSummary inspect(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "urls 不能为空");
        }
        List<String> targets = urls.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        if (targets.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "urls 不能为空");
        }

        // 地址少就少开线程，地址多也不能超过配置上限
        int concurrency = Math.min(targets.size(),
                Math.max(1, properties.getInspect().getMaxConcurrency()));
        ExecutorService pool = ThreadUtil.newExecutor(concurrency);
        List<InspectResult> results = new ArrayList<>();

        try {
            List<Future<InspectResult>> futures = new ArrayList<>();
            for (String url : targets) {
                futures.add(pool.submit(() -> probe(url)));
            }
            for (Future<InspectResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    // 单个任务异常不能拖垮整批
                    results.add(new InspectResult("unknown", 0, 0, false, e.getMessage()));
                }
            }
        } finally {
            // 线程池不关，请求结束后线程还在
            pool.shutdown();
        }

        results.sort(Comparator.comparingLong(InspectResult::costMs).reversed());
        int success = (int) results.stream().filter(InspectResult::ok).count();
        long avg = (long) results.stream().mapToLong(InspectResult::costMs).average().orElse(0);

        return new InspectSummary(results.size(), success, results.size() - success,
                avg, results);
    }

    private InspectResult probe(String url) {
        if (!Validator.isUrl(url)) {
            return new InspectResult(url, 0, 0, false, "地址格式不正确");
        }

        long start = System.currentTimeMillis();
        try {
            int status = HttpRequest.get(url)
                    .timeout(properties.getInspect().getTimeoutMillis())
                    .execute()
                    .getStatus();
            long cost = System.currentTimeMillis() - start;
            // 4xx / 5xx 连接是通的但接口已经不可用，算失败
            return new InspectResult(url, status, cost,
                    status >= 200 && status < 400, null);
        } catch (Exception e) {
            return new InspectResult(url, 0, System.currentTimeMillis() - start,
                    false, StrUtil.maxLength(e.getMessage(), 120));
        }
    }
}
