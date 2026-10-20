package top.zff06.redis.dto;

/**
 * @auther zff06
 * @data 2026/10/14
 * @description 缓存接口的统一返回体，带数据来源与耗时
 * <p>
 * source 取值：
 * db            这次查了数据库
 * cache         命中缓存
 * cache-empty   命中的是空值标记，库里确实没有这条记录
 * db-fallback   没抢到重建锁、等了一下仍没读到缓存，退回查库
 * cache-invalid 缓存里的值不是 Goods，序列化配置有问题时才会出现
 **/
public record CacheResult<T>(T data, String source, double costMs) {

    /** 把纳秒起点换算成毫秒耗时，省得每个方法里重复算 */
    public static double costFrom(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }
}
