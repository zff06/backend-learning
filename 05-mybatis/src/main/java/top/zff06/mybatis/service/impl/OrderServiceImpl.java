package top.zff06.mybatis.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.zff06.mybatis.entity.Course;
import top.zff06.mybatis.entity.Order;
import top.zff06.mybatis.mapper.OrderMapper;
import top.zff06.mybatis.service.CourseService;
import top.zff06.mybatis.service.OrderService;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @auther zff06
 * @data 2026/10/11
 * @description OrderServiceImpl 下单业务：扣库存与建订单必须同生共死
 **/
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    /** 每个学分 99 元，金额规则随意，目的是让 amount 不是手动写死的常量 */
    private static final BigDecimal PRICE_PER_CREDIT = new BigDecimal("99.00");

    private final CourseService courseService;

    /**
     * 加 @Transactional 后，本方法内的多次写库共用同一个连接、同一个事务：
     * 任何一处抛异常，前面已经执行过的 SQL 全部回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean createOrder(Long courseId, Integer quantity) {
        if (courseId == null) {
            throw new IllegalArgumentException("课程 id 不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("购买数量必须大于 0");
        }

        Course course = courseService.getById(courseId);
        if (course == null) {
            throw new IllegalArgumentException("课程不存在：" + courseId);
        }

        // 扣库存：减法交给数据库做（stock = stock - n），避免"先查后改"的覆盖写；
        // ge(stock, quantity) 是乐观条件，库存不够时这条 UPDATE 影响 0 行，永远扣不成负数
        boolean deducted = courseService.update(new LambdaUpdateWrapper<Course>()
                .eq(Course::getId, courseId)
                .ge(Course::getStock, quantity)
                .setSql("stock = stock - " + quantity));
        if (!deducted) {
            // 抛运行时异常，让事务把上面那句 UPDATE 一起撤销
            throw new RuntimeException("库存不足：课程 " + courseId
                    + " 剩余 " + course.getStock() + "，本次要买 " + quantity);
        }

        Order order = new Order();
        order.setOrderNo(nextOrderNo());
        order.setCourseId(courseId);
        order.setQuantity(quantity);
        // 金额 = 学分 × 单价 × 数量（规则随意，只为让 amount 由业务算出而不是写死）
        order.setAmount(PRICE_PER_CREDIT
                .multiply(BigDecimal.valueOf(course.getCredit() == null ? 1 : course.getCredit()))
                .multiply(BigDecimal.valueOf(quantity)));
        order.setStatus(1);
        // createTime / updateTime / deleted 交给 MyMetaObjectHandler 填充
        return save(order);
    }

    @Override
    public Course courseInfo(Long courseId) {
        return courseService.getById(courseId);
    }

    /** 订单号：时间戳 + 三位随机数，避免同一毫秒下的唯一索引冲突 */
    private String nextOrderNo() {
        return "ORD" + System.currentTimeMillis()
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }
}
