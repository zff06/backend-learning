package top.zff06.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 订单接口的集成测试：3 个用例。
 *
 * <p>补单元测试和切片测试各自的盲区：
 * 单元测试不知道 OrderService 有没有被注册成 Bean；
 * 切片测试里 OrderService 是假的，不知道真实规则接上 Web 层之后还对不对。
 *
 * <p>@SpringBootTest 启动完整容器（不监听端口），@AutoConfigureMockMvc 补上 MockMvc。
 * webEnvironment 用默认的 MOCK：走的是同一套 DispatcherServlet、过滤器链、消息转换器，
 * 省掉端口绑定和网络往返。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description OrderIntegrationTest
 **/
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("订单接口（集成测试）")
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 和切片测试的 calculateOk 几乎一样，只是断言更全——这一次数字是真的由 OrderService 算出来的。
     */
    @Test
    @DisplayName("完整链路：请求 → Service → 响应")
    void calculateThroughRealStack() throws Exception {
        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1, "amount": 300.00, "memberLevel": "GOLD", "quantity": 1}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.originAmount").value(300.00))
                .andExpect(jsonPath("$.data.discount").value(53.00))
                .andExpect(jsonPath("$.data.finalAmount").value(247.00))
                .andExpect(jsonPath("$.data.appliedRules.length()").value(2));
    }

    @Test
    @DisplayName("快捷试算接口可用")
    void quickCalculate() throws Exception {
        mockMvc.perform(get("/orders/quick-calculate")
                        .param("amount", "150")
                        .param("memberLevel", "NORMAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finalAmount").value(140.00));
    }

    /**
     * 不 mock 任何东西，靠 DefaultInventoryClient 固定返回 100。买 200 件必然被拒。
     * 哪天有人把 DefaultInventoryClient 改成返回 1000，这条就会红——这正是它该做的。
     */
    @Test
    @DisplayName("真实库存客户端返回 100，买 200 件会被拒")
    void stockNotEnoughThroughRealClient() throws Exception {
        mockMvc.perform(post("/orders/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1, "amount": 100, "quantity": 200}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(30409));
    }
}
