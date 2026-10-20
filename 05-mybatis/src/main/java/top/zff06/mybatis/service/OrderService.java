package top.zff06.mybatis.service;

import com.baomidou.mybatisplus.spring.service.IService;
import top.zff06.mybatis.entity.Course;
import top.zff06.mybatis.entity.Order;

/**
 * @auther zff06
 * @data 2026/10/11
 * @description OrderService
 **/
public interface OrderService extends IService<Order> {

    /**
     * 下单：扣课程库存 + 生成订单，两个写操作在同一事务里，要么都成功要么都回滚
     *
     * @param courseId 课程 id
     * @param quantity 购买数量
     * @return 订单是否创建成功
     */
    boolean createOrder(Long courseId, Integer quantity);

    /** 查询课程（含库存），演示时用来看库存变化 */
    Course courseInfo(Long courseId);
}
