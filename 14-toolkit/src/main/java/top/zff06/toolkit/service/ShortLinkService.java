package top.zff06.toolkit.service;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.zff06.toolkit.common.BusinessException;
import top.zff06.toolkit.common.ErrorCode;
import top.zff06.toolkit.config.ToolkitProperties;
import top.zff06.toolkit.dto.ShortLinkResponse;
import top.zff06.toolkit.dto.ShortLinkStats;
import top.zff06.toolkit.support.ShortLink;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description ShortLinkService
 **/
@Service
@RequiredArgsConstructor
public class ShortLinkService {

    /**
     * 去掉容易看混的 0O1lI
     */
    private static final String ALPHABET =
            "23456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final int MAX_RETRY = 5;

    private final ToolkitProperties properties;

    private TimedCache<String, ShortLink> cache;

    @PostConstruct
    void init() {
        cache = CacheUtil.newTimedCache(properties.getShortLink().getExpireSeconds() * 1000);
        cache.schedulePrune(60_000);
    }

    @PreDestroy
    void destroy() {
        cache.cancelPruneSchedule();
        cache.clear();
    }

    public ShortLinkResponse create(String url, Long ttlSeconds) {
        if (!Validator.isUrl(url)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "url 不是合法的地址");
        }
        long ttl = ttlSeconds == null || ttlSeconds <= 0
                ? properties.getShortLink().getExpireSeconds()
                : ttlSeconds;

        String code = allocateCode();
        long expireAt = System.currentTimeMillis() + ttl * 1000;
        cache.put(code, new ShortLink(code, url, expireAt), ttl * 1000);

        String baseUrl = StrUtil.removeSuffix(properties.getShortLink().getBaseUrl(), "/");
        return new ShortLinkResponse(code, baseUrl + "/toolkit/s/" + code, url, expireAt);
    }

    /**
     * 解析短码，点一次算一次
     */
    public String resolve(String code) {
        ShortLink link = cache.get(code, false);
        if (link == null) {
            throw new BusinessException(ErrorCode.SHORT_LINK_NOT_FOUND);
        }
        link.getClicks().incrementAndGet();
        return link.getUrl();
    }

    public ShortLinkStats stats(String code) {
        ShortLink link = cache.get(code, false);
        if (link == null) {
            throw new BusinessException(ErrorCode.SHORT_LINK_NOT_FOUND);
        }
        return new ShortLinkStats(link.getCode(), link.getUrl(), link.getClicks().get(),
                link.getCreatedAt(), link.getExpireAt());
    }

    /**
     * 生成一个没被占用的短码
     */
    private String allocateCode() {
        for (int i = 0; i < MAX_RETRY; i++) {
            String code = RandomUtil.randomString(ALPHABET, CODE_LENGTH);
            if (!cache.containsKey(code)) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "短码分配失败，请重试");
    }
}
