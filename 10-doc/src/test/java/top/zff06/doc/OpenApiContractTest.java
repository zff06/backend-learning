package top.zff06.doc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 练习 3：把文档校验接进 CI
 * <p>
 * 做法：把当前代码生成的 openapi.json 规范成"key 排序 + 固定缩进"的文本，
 * 与仓库里的基线 src/test/resources/openapi-baseline.json 逐行比较；不一致就失败。
 * 失败信息里明确写"接口契约有变更，请确认是否需要通知调用方"。
 * <p>
 * 为什么不直接 curl 应用导出的文件：测试进程里拿到的文档和 CI 跑的是同一份代码，
 * 少一个"先起服务再测"的步骤，也不会因为 8010 被自己的 IDEA 实例占着而连错进程。
 * <p>
 * 为什么要排序：path / schema / property 都是 Map，迭代顺序不受控，
 * 加一个字段就可能让后面所有行整体错位，不排序会被顺序变化误触发。
 * <p>
 * 基线需要更新时：mvn -pl 10-doc test -Ddoc.baseline.update=true
 * 然后 review src/test/resources/openapi-baseline.json 的 diff 并一起提交。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description OpenApiContractTest
 **/
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest {

    private static final Path BASELINE = Path.of("src/test/resources/openapi-baseline.json");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    @DisplayName("接口契约基线比对：契约变更必须显式确认")
    void contractShouldMatchBaseline() throws Exception {
        String raw = mockMvc.perform(get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String actual = prettySorted(jsonMapper.readTree(raw));

        if (Boolean.getBoolean("doc.baseline.update") || !Files.exists(BASELINE)) {
            Files.createDirectories(BASELINE.getParent());
            Files.writeString(BASELINE, actual, StandardCharsets.UTF_8);
            return;
        }

        List<String> expected = Files.readAllLines(BASELINE, StandardCharsets.UTF_8);
        List<String> got = actual.lines().toList();
        List<String> diff = diff(expected, got);
        if (!diff.isEmpty()) {
            fail("""
                    接口契约有变更，请确认是否需要通知调用方。
                    若变更是预期内的，执行 mvn -pl 10-doc test -Ddoc.baseline.update=true 更新基线，并和代码一起提交。

                    """ + String.join("\n", diff.subList(0, Math.min(diff.size(), 40))));
        }
        assertTrue(diff.isEmpty());
    }

    /**
     * 递归排序 key 后输出：对象 key 升序，数组保持原顺序
     * <p>
     * 顺手剔掉 servers：它的 url 是"当前实例地址 + 端口"，MockMvc 下是 http://localhost，
     * 真起服务时变成 http://localhost:8090，不属于接口契约，留着会让基线永远对不上。
     */
    private String prettySorted(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        write(node, sb, 0);
        return sb.append('\n').toString();
    }

    private void write(JsonNode node, StringBuilder sb, int indent) {
        String pad = "  ".repeat(indent);
        String padIn = "  ".repeat(indent + 1);
        if (node.isObject()) {
            Map<String, JsonNode> sorted = new TreeMap<>();
            for (Map.Entry<String, JsonNode> e : node.properties()) {
                if (indent == 0 && "servers".equals(e.getKey())) {
                    continue;
                }
                sorted.put(e.getKey(), e.getValue());
            }
            if (sorted.isEmpty()) {
                sb.append("{}");
                return;
            }
            sb.append("{\n");
            List<String> keys = new ArrayList<>(sorted.keySet());
            for (int i = 0; i < keys.size(); i++) {
                sb.append(padIn).append('"').append(keys.get(i)).append("\": ");
                write(sorted.get(keys.get(i)), sb, indent + 1);
                sb.append(i == keys.size() - 1 ? "\n" : ",\n");
            }
            sb.append(pad).append('}');
        } else if (node.isArray()) {
            if (node.isEmpty()) {
                sb.append("[]");
                return;
            }
            sb.append("[\n");
            for (int i = 0; i < node.size(); i++) {
                sb.append(padIn);
                write(node.get(i), sb, indent + 1);
                sb.append(i == node.size() - 1 ? "\n" : ",\n");
            }
            sb.append(pad).append(']');
        } else if (node.isString()) {
            sb.append('"').append(escape(node.asString())).append('"');
        } else {
            sb.append(node.toString());
        }
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private List<String> diff(List<String> expected, List<String> actual) {
        List<String> result = new ArrayList<>();
        int max = Math.max(expected.size(), actual.size());
        for (int i = 0; i < max; i++) {
            String e = i < expected.size() ? expected.get(i) : "<缺失>";
            String a = i < actual.size() ? actual.get(i) : "<缺失>";
            if (!e.equals(a)) {
                result.add("第 " + (i + 1) + " 行\n  - " + e + "\n  + " + a);
            }
        }
        return result;
    }
}
