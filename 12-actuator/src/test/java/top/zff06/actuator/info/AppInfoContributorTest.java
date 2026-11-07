package top.zff06.actuator.info;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 应用信息贡献者测试：MockEnvironment 有真的 getProperty 实现，比 Mockito 省一堆 when。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description AppInfoContributorTest
 **/
@DisplayName("应用信息")
class AppInfoContributorTest {

    @Test
    @DisplayName("塞进去的模块信息与负责人信息都在")
    @SuppressWarnings("unchecked")
    void contributesModuleAndOwner() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.application.name", "12-应用监控")
                .withProperty("server.port", "8012");
        AppInfoContributor contributor = new AppInfoContributor(environment);

        Info.Builder builder = new Info.Builder();
        contributor.contribute(builder);
        Info info = builder.build();

        assertThat(info.getDetails()).containsKeys("module", "owner");

        Map<String, Object> module = (Map<String, Object>) info.getDetails().get("module");
        assertThat(module)
                .containsEntry("name", "12-应用监控")
                .containsEntry("port", "8012")
                .containsKey("startedAt");

        Map<String, Object> owner = (Map<String, Object>) info.getDetails().get("owner");
        assertThat(owner).containsEntry("team", "backend-learning");
    }
}
