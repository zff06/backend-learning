package top.zff06.test.client;

import org.springframework.stereotype.Component;

/**
 * 默认库存客户端：固定返回 100 件。
 *
 * <p>不接数据库、不调远程服务，应用可以独立跑起来。集成测试里"买 200 件必然被拒"
 * 这条用例就是靠它固定返回 100 撑住的。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description DefaultInventoryClient
 **/
@Component
public class DefaultInventoryClient implements InventoryClient {

    @Override
    public int availableStock(Long productId) {
        return 100;
    }
}
