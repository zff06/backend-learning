package top.zff06.toolkit.dto;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description OrderRow
 **/
@Data
public class OrderRow {

    @Alias("订单号")
    private String orderNo;

    @Alias("客户")
    private String customer;

    @Alias("手机号")
    private String phone;

    @Alias("金额")
    private String amount;

    @Alias("状态")
    private String status;

    @Alias("下单时间")
    private String createdAt;
}
