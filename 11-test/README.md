# 11-test · Spring Boot 测试综合练习

订单试算模块，用三种测试守三类东西：

| 测试 | 类 | 用例 | 守什么 |
|---|---|---|---|
| 单元测试 | `OrderServiceTest` | 24 | 满减 / 会员 / 边界的分支穷举，直接 `new`，依赖 mock |
| 切片测试 | `OrderControllerTest` | 12 | 参数绑定、`@Valid`、异常映射、JSON 序列化 |
| 集成测试 | `OrderIntegrationTest` | 3 | Bean 装配与真实链路 |

合计 39 个用例，`mvn -pl 11-test test` 一次跑完，约 19 秒（首次含编译）。

## 跑

```bash
# 全部
mvn -pl 11-test test

# 单个类
mvn -pl 11-test test -Dtest=OrderServiceTest

# 单个 @Nested 内部类（$ 必须加引号，否则 shell 会展开）
mvn -pl 11-test test -Dtest='OrderServiceTest$MemberRules'

# @Nested 里的单个方法：类名和内部类都要写全
mvn -pl 11-test test -Dtest='OrderServiceTest$MemberRules#goldMemberJustBelow300'

# 顶层类的单个方法
mvn -pl 11-test test -Dtest=OrderIntegrationTest#quickCalculate
```

覆盖率报告：`11-test/target/site/jacoco/index.html`，原始数据在同目录 `jacoco.csv`。

## 业务规则

| 规则 | 条件 | 优惠 |
|---|---|---|
| 满 500 减 80 | 金额 ≥ 500 | 减 80 |
| 满 300 减 40 | 300 ≤ 金额 < 500 | 减 40 |
| 满 100 减 10 | 100 ≤ 金额 < 300 | 减 10 |
| 金卡折扣 | `memberLevel` 为 `GOLD`（忽略大小写） | 满减之后再打 95 折 |
| 最低金额 | 任何情况 | 应付不低于 0.01 |

满减三档取其一，不叠加。金卡的计算顺序不能反：300 元金卡先减 40 得 260，再打 95 折，应付 247；顺序反过来是 245。

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/orders/calculate` | JSON 请求体，带 `@Valid` |
| GET | `/orders/quick-calculate?amount=&memberLevel=` | 查询参数，**不走** `@Valid`，由 Service 兜底 |
| GET | `/orders/hello` | 模块信息 |

启动：`mvn -pl 11-test spring-boot:run`，端口 8011。

### 分层返回业务码

`GlobalExceptionHandler` 用 `e.getCode() % 1000` 把业务码后三位当 HTTP 状态：

| 入口 | 谁拦下的 | HTTP | 业务码 |
|---|---|---|---|
| `POST /calculate` 传 -1 | `@Valid` + `@DecimalMin` | 400 | 400 |
| `GET /quick-calculate` 传 0 | `OrderService` 里的判断 | 400 | 30400 |
| 库存不足（买 200 库存 100） | `BusinessException.stockNotEnough` | 409 | 30409 |

注意 30400 映射成 400，不是 409——只有 30409 的后三位才是 409。

## 实测覆盖率

`mvn -pl 11-test clean test` 后从 `jacoco.csv` 汇总（已排除 `TestApplication`）：

| 指标 | 结果 |
|---|---|
| 行覆盖 | 100%（75/75） |
| 分支覆盖 | 95.0%（19/20） |
| 指令覆盖 | 100%（380/380） |
| 复杂度覆盖 | 97.1%（33/34） |

剩余 1 个未覆盖分支在 `OrderRequest` 的 `memberLevel.isBlank()`：所有测试传的都是 `null` 或非空串，没有传过 `""` 或 `"   "`（`null` 走的是 `== null` 那一半）。

> POM 里 `jacoco` 配了 `<exclude>top/zff06/test/TestApplication.class</exclude>`。
> 不排除的话行覆盖是 97.1%（68/70），少的 2 行是 `main` 里的 `SpringApplication.run(...)`——测试永远调不到它。

## 练习答案（教程第 10 节）

### 10.1 随堂练习

**去掉 `@Valid`**：`OrderControllerTest` 里 `calculateBadRequest` 和 `calculateMissingField` 两条变红，其余 10 条仍绿。

实测报错：

```
Status expected:<400> but was:<200>
```

`amount: -1` 一路走到 `OrderService` 被 `compareTo(ZERO) <= 0` 拦下，但因为是 Mock 掉的 Service，直接返回了默认对象，所以是 200——红的不是"没拦住"，而是"没人拦、也没映射成 400"。

**加"满 500 减 80"**：先写 `discountAt500` 断言 `discount=80 / finalAmount=420`，跑一次红；再加规则，`discountAt500` 绿，但 `finalAmountMatrix` 里 `1000.00 → 960.00` 立刻红（1000 现在该减 80，应付 920）。参数化测试的价值就在这里：改规则会一次暴露所有受影响的用例。已把该行改成 `1000.00, 920.00`，并补了 `499.99, 459.99` 和 `500.00, 420.00` 两个边界。

**`setUp` 里改成 `thenReturn(0)`**：16 个用例里 14 个出错，全部是"买 1 件却库存不足"。剩下 2 个是 `stockNotEnough`（本来就 mock 成 2）和 `nullAmount` / `zeroAmount`（在查库存之前就抛了）。`@BeforeEach` 的默认值决定了"正常情况"是什么，改一个字影响全局。

### 10.2 课后练习

| # | 练习 | 落点 |
|---|---|---|
| 1 | 加 `unexpectedError` | `OrderControllerTest#unexpectedError`，行覆盖 97.1% → 100% |
| 2 | 加 `nullAmount` | `EdgeCases#nullAmount`，`OrderService` 分支覆盖 → 100% |
| 3 | 299.99 + 金卡组合 | `MemberRules#goldMemberJustBelow300`，断言 24.50 / 275.49 |
| 4 | 会员等级参数化 | `MemberRules#memberLevelMatrix`，5 组数据含 `null` |
| 5 | `quick-calculate` 金额 0 | `OrderControllerTest#quickCalculateZeroAmount`，断 400 + 30400 |
| 6 | `memberLevel=DIAMOND` | `OrderControllerTest#quickCalculateUnknownMemberLevel`，200 且不打折 |

**练习 2 的答案**：`nullAmount` 属于三层校验的**第三层**（Service 内的判断），不是 `@NotNull` 那一层。
它证明的是：绕过 Web 层直接调 Service 时，空金额会被业务异常拦住，而不是抛 NPE——定时任务、消息消费者这类没有 `@Valid` 的入口靠的就是这一句。
它没证明的是：Web 层会不会把 null 传进来，那由 `@NotNull` 负责。

**练习 4 的两种写法**：`@CsvSource(nullValues = "null")` 能跑通，把字面量 `"null"` 转成真正的 null；`@NullSource` 只能给整个方法**补一条**空值用例，不能和 `@CsvSource` 混在同一组数据里逐行给。所以这里用前一种。

**练习 6 的判断**：选择了"宽容"——未知等级静默按普通用户处理，用例记录现状。
理由：下单是主流程，前端拼错等级不该让用户下不了单。代价是 `"GOLD "`（带空格）这种拼错也会静默不打折，用户以为打了折其实没打。想改成严格模式，在 `OrderService` 里对非 `GOLD`/`NORMAL` 抛业务异常即可，届时 `quickCalculateUnknownMemberLevel` 会变红提醒确认。

## 踩到的坑

| 现象 | 原因 | 解决 |
|---|---|---|
| `-Dtest=OrderServiceTest#nullAmount` 报 `Tests run: 0` 但 BUILD SUCCESS | 方法在 `@Nested` 内部类里，Surefire 要求类名和内部类都写全 | `-Dtest='OrderServiceTest$EdgeCases#nullAmount'` |
| `-Dtest='OrderServiceTest#*Member*'` 同样 0 个 | 同上，模式只能匹配到顶层类的方法 | 先定位到内部类：`-Dtest='OrderServiceTest$MemberRules'` |
| 中文测试名在控制台乱码 | Git Bash 输出编码 | 加 `-Dfile.encoding=UTF-8` |

`-Dtest` 匹配不到方法时 Surefire **不会失败**，只在 `Tests run: 0` 里静默放过——写测试时最好扫一眼这个数字。

## Boot 4 的包路径

| 类 | Boot 3 | Boot 4 |
|---|---|---|
| `@WebMvcTest` / `@AutoConfigureMockMvc` | `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` |
| `@MockBean` | `org.springframework.boot.test.mock.mockito` | 已删除，换 `org.springframework.test.context.bean.override.mockito.MockitoBean` |
| `TestRestTemplate` | `org.springframework.boot.test.web.client` | `org.springframework.boot.resttestclient` |
