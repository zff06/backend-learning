package top.zff06.test.client;

/**
 * 库存查询客户端。
 *
 * <p>真实环境里这是一次远程调用（HTTP 或 RPC）。抽成接口而不是在 Service 里 new 一个实现，
 * 是为了让依赖注入把"库存从哪来"这件事从 OrderService 里摘出去——测试才能把它替换掉。
 * Mockito 只能 mock 接口和 non-final 类，接口是前提。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description InventoryClient
 **/
public interface InventoryClient {

    /**
     * 查询某件商品的可售库存。
     *
     * @param productId 商品 ID
     * @return 可售数量
     */
    int availableStock(Long productId);
}
