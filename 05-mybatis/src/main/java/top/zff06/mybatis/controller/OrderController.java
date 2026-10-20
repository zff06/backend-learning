package top.zff06.mybatis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.mybatis.entity.Course;
import top.zff06.mybatis.entity.Order;
import top.zff06.mybatis.service.OrderService;

import java.util.List;

/**
 * @auther zff06
 * @data 2026/10/11
 * @description OrderController
 **/
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /* 下单：POST /orders?courseId=1&quantity=2 */
    @PostMapping
    public boolean create(@RequestParam Long courseId, @RequestParam Integer quantity) {
        return orderService.createOrder(courseId, quantity);
    }

    /* 查订单列表（逻辑删除的行自动过滤掉） */
    @GetMapping
    public List<Order> list() {
        return orderService.list();
    }

    @GetMapping("/{id}")
    public Order getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    /* 看课程库存，演示"扣库存"与"回滚后库存不变"时用 */
    @GetMapping("/course/{courseId}")
    public Course course(@PathVariable Long courseId) {
        return orderService.courseInfo(courseId);
    }

    /* 删除订单：走逻辑删除，SQL 是 UPDATE t_order SET deleted = 1 */
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return orderService.removeById(id);
    }
}
