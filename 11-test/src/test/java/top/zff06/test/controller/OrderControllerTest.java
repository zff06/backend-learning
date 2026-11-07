package top.zff06.test.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.zff06.test.common.BusinessException;
import top.zff06.test.dto.OrderResult;
import top.zff06.test.service.OrderService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 订单接口的 Web 层切片测试：9 个用例。
 *
 * <p>Spring 只装配 Web 层，OrderService 用 @MockitoBean 换成假的。
 * 测的是参数绑定、@Valid 校验、异常映射、JSON 序列化这几层——
 * 它们的出错方式比业务逻辑更隐蔽：算错了一眼能看出来，@Valid 忘写了没人发现。
 *
 * <p>Boot 4 注意：@WebMvcTest 与 @AutoConfigureMockMvc 都在
 * {@code org.springframework.boot.webmvc.test.autoconfigure}；
 * @MockBean 已删除，换成 Spring Framework 的 @MockitoBean。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description OrderControllerTest
 **/
@WebMvcTest(OrderController.class)
@DisplayName("订单接口（Web 层切片）")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    @DisplayName("正常试算返回 200 与计算结果")
    void calculateOk() throws Exception {
        given(orderService.calculate(any())).willReturn(new OrderResult(
                new BigDecimal("300.00"), new BigDecimal("53.00"),
                new BigDecimal("247.00"), List.of("满 300 减 40", "金卡会员 95 折")));

        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1, "amount": 300.00, "memberLevel": "GOLD", "quantity": 1}"""))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.finalAmount").value(247.00))
                .andExpect(jsonPath("$.data.appliedRules.length()").value(2));
    }

    /**
     * 断言的文案与 OrderRequest 上 @DecimalMin 的 message 一字不差。
     * 两处是耦合的——改注解里的文案，测试就会红。这正是想要的效果。
     */
    @Test
    @DisplayName("参数校验失败返回 400 与具体原因")
    void calculateBadRequest() throws Exception {
        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1, "amount": -1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("金额必须大于 0"));
    }

    @Test
    @DisplayName("缺少必填字段返回 400")
    void calculateMissingField() throws Exception {
        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 100}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户 ID 不能为空"));
    }

    /**
     * 整组里最有价值的一条：Service 抛业务码 30409，GlobalExceptionHandler 算
     * 30409 % 1000 = 409，ResponseEntity.status(409) 才是真正的 HTTP 状态。任何一环断了都会红。
     */
    @Test
    @DisplayName("业务异常按业务码映射成 409")
    void calculateStockNotEnough() throws Exception {
        given(orderService.calculate(any())).willThrow(BusinessException.stockNotEnough(5));

        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1, "amount": 100, "quantity": 5}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(30409));
    }

    /**
     * 不接 HttpMessageNotReadableException 的话，解析失败会落到兜底的 Exception 分支返回 500，
     * 前端会以为是服务端崩了，实际是请求格式错了。
     */
    @Test
    @DisplayName("请求体不是合法 JSON 返回 400")
    void calculateBrokenJson() throws Exception {
        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 1, \"amount\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请求体格式错误"));
    }

    @Test
    @DisplayName("模块信息接口返回提示")
    void hello() throws Exception {
        mockMvc.perform(get("/orders/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.module").value("11-test"));
    }

    @Test
    @DisplayName("快捷试算走查询参数")
    void quickCalculate() throws Exception {
        given(orderService.calculate(any())).willReturn(new OrderResult(
                new BigDecimal("100.00"), new BigDecimal("10.00"),
                new BigDecimal("90.00"), List.of("满 100 减 10")));

        mockMvc.perform(get("/orders/quick-calculate")
                        .param("amount", "100")
                        .param("memberLevel", "NORMAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finalAmount").value(90.00));
    }

    /**
     * 补处理器之前这条是 500——客户端传错参数、服务端报"系统繁忙"。
     */
    @Test
    @DisplayName("查询参数类型不对返回 400 而不是 500")
    void quickCalculateBadType() throws Exception {
        mockMvc.perform(get("/orders/quick-calculate").param("amount", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数 amount 格式不正确"));
    }

    @Test
    @DisplayName("缺必填查询参数返回 400")
    void quickCalculateMissingParam() throws Exception {
        mockMvc.perform(get("/orders/quick-calculate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("缺少参数 amount"));
    }

    /**
     * 练习 10.2-1：补上 GlobalExceptionHandler 的兜底分支。
     *
     * <p>willThrow 之后 Spring 会把异常交给 @ExceptionHandler(Exception.class)，
     * handleOther 里有一行 log.error，控制台会打一整段堆栈——这是正常的，不是测试失败。
     */
    @Test
    @DisplayName("未预期异常返回 500 与统一结构")
    void unexpectedError() throws Exception {
        given(orderService.calculate(any())).willThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1, "amount": 100}"""))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统繁忙，请稍后再试"));
    }

    /**
     * 练习 10.2-5：quick-calculate 不走 @Valid，金额 0 由 Service 兜底拦下。
     *
     * <p>断的是 isBadRequest() 而不是 isConflict()：业务码 30400 的后三位是 400，
     * 只有 30409 的后三位才是 409。写之前按 e.getCode() % 1000 自己算一遍再跑。
     */
    @Test
    @DisplayName("快捷试算金额为 0 由 Service 兜底，业务码 30400 映射成 400")
    void quickCalculateZeroAmount() throws Exception {
        given(orderService.calculate(any())).willThrow(BusinessException.amountInvalid());

        mockMvc.perform(get("/orders/quick-calculate").param("amount", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(30400))
                .andExpect(jsonPath("$.message").value("金额必须大于 0"));
    }

    /**
     * 练习 10.2-6：未知会员等级既不报错也不打折，被静默当成普通用户。
     * 这是一条"记录现状"的用例——哪天决定改成严格模式，它会红，提醒你去确认行为变更。
     */
    @Test
    @DisplayName("未知会员等级被当成普通用户处理")
    void quickCalculateUnknownMemberLevel() throws Exception {
        given(orderService.calculate(any())).willReturn(new OrderResult(
                new BigDecimal("300.00"), new BigDecimal("40.00"),
                new BigDecimal("260.00"), List.of("满 300 减 40")));

        mockMvc.perform(get("/orders/quick-calculate")
                        .param("amount", "300")
                        .param("memberLevel", "DIAMOND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appliedRules.length()").value(1));
    }
}
