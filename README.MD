# backend-learning

后端工程化多模块学习仓库：以 Spring Boot 为主线，按知识点拆成 15 个独立模块，从快速入门一路进阶到工具链实战，另含一个综合练习模块和一套前后端分离实战项目（知乎专栏）。

## 技术栈

| 类别 | 技术 |
|---|---|
| 语言 / JDK | Java 21 |
| 框架 | Spring Boot 4.1.1（Maven 多模块，父 POM 统一管理） |
| ORM | MyBatis-Plus + Druid 连接池 + MySQL |
| 缓存 | Spring Data Redis + Redisson + Spring Cache |
| 工具链 | Hutool、POI（Excel）、zxing（二维码）、oshi（系统信息）、JWT、验证码 |
| 前端实战 | Vue 3 + TypeScript + Vite |

## 环境要求

- JDK 21、Maven 3.9+
- MySQL 8（mybatis / redis / zhihu 模块需要）
- Redis 6+（redis、test-redis1 模块需要）

## 目录结构

```
backend-learning
├── 01-quickstart    # 快速入门：第一个 Controller
├── 02-config        # 配置管理
├── 03-logging       # 日志体系
├── 04-web           # Web 开发规范
├── 05-mybatis       # 数据库访问
├── 06-redis         # 缓存与 Redis 应用
├── 07-mq            # 消息队列（规划中）
├── 08-schedule      # 定时任务
├── 09-security      # 安全认证（骨架）
├── 10-file          # 文件处理（骨架）
├── 11-doc           # 接口文档（骨架）
├── 12-test          # 单元测试（骨架）
├── 13-actuator      # 应用监控（骨架）
├── 14-deploy        # 部署上线（骨架）
├── 15-tollkit       # 工具链实战（综合）
├── test-redis1      # Redis 综合练习
├── zhihu-api        # 实战：知乎专栏后端
├── zhihu-app        # 实战：知乎专栏前端（Vue 3 + TS）
└── my-vue-app       # Vue 3 前端练习
```

## 学习路线与模块详情

建议按编号顺序推进，前 8 个模块是核心主线：

| 模块 | 主题 | 状态 | 核心学习点 |
|---|---|---|---|
| 01-quickstart | 快速入门 | ✅ | Spring Boot 项目创建、`@RestController`、`@GetMapping`、实体类 |
| 02-config | 配置管理 | ✅ | yml 多环境配置（dev/prod）、`@ConfigurationProperties`、`@ConditionalOnProperty` 按环境装配 Bean、配置类单元测试 |
| 03-logging | 日志体系 | ✅ | Logback（`logback-spring.xml`）、日志脱敏工具（`LogMaskUtil`）、拦截器（`FilterConfig`） |
| 04-web | Web 开发规范 | ✅ | 统一返回 `Result<T>`、分页 `PageResult`、错误码 `ErrorCode`、全局异常处理 `@RestControllerAdvice`、DTO 分层、参数校验 |
| 05-mybatis | 数据库访问 | ✅ | MyBatis-Plus CRUD、分页插件、自动填充（`MyMetaObjectHandler`）、多表关联（学生/课程/成绩）、VO 统计查询、Druid 连接池（含连接池耗尽压测）、Mapper/Service 层单测 |
| 06-redis | 缓存与 Redis | ✅ | 五大基本数据类型操作、Spring Cache 注解缓存、缓存与 DB 一致性、库存扣减、分布式锁（Redisson）、接口限流、排行榜（ZSet）、短信验证码登录（荣联云 SMS）、Redis 单测 |
| 07-mq | 消息队列 | 🚧 | 规划中，尚未引入 MQ 依赖 |
| 08-schedule | 定时任务 | ✅ | `@Scheduled` cron 表达式、`@EnableScheduling`、任务线程、HTTP 调用第三方 API（okhttp + fastjson2 天气接口）、spring-boot-starter-mail 邮件发送 |
| 09-security | 安全认证 | 🚧 | 骨架（JWT 登录可在 test-redis1 先行参考） |
| 10-file | 文件处理 | 🚧 | 骨架（文件上传实战可参考 15-tollkit） |
| 11-doc | 接口文档 | 🚧 | 骨架 |
| 12-test | 单元测试 | 🚧 | 骨架（单测实践已在 02/05/06 模块落地） |
| 13-actuator | 应用监控 | 🚧 | 骨架 |
| 14-deploy | 部署上线 | 🚧 | 骨架 |
| 15-tollkit | 工具链综合实战 | ✅ | 验证码生成、短信验证码、JWT 签名、图形/敏感词检测、文本提取、Excel 导入导出（POI）、分片上传、短链接服务、扫码登录、海报生成（zxing + hutool）、系统信息（oshi） |

> 注：模块目录 `15-tollkit` 实际内容是 toolkit（工具链），目录名拼写少了一个 i。

## 实战与练习项目

### test-redis1（Redis 综合练习）

整合 Redis + 短信 SDK + JWT 的独立练习模块：验证码登录、JWT 令牌签发与校验、Redis 缓存实战。

### zhihu-api + zhihu-app（前后端分离实战）

- **zhihu-api**：知乎专栏后端。Spring Boot + MyBatis-Plus + MySQL，实现专题（Special）的完整 CRUD（Controller → Service → Mapper 分层），含 Service 层测试。
- **zhihu-app**：对应前端，Vue 3 + TypeScript + Vite。

这套项目用来打通"数据库设计 → 接口开发 → 前端联调"的完整链路。

## 构建与运行

```bash
# 编译整个仓库（在根目录执行）
mvn clean install -DskipTests

# 运行单个模块（以 04-web 为例）
mvn spring-boot:run -pl 04-web

# 打包部署
mvn clean package -pl 04-web
# 产物：04-web/target/*.jar
```

需要外部服务的模块启动前先确认 MySQL / Redis 已就绪，并检查各模块 `src/main/resources/application*.yml` 中的连接配置。

## 约定

- 统一包结构：`top.zff06.<模块名>`，子包按 `controller / service / mapper / entity / dto / vo / config / common` 分层。
- 根 POM 统一继承 `spring-boot-starter-parent`，公共依赖（webmvc、lombok、devtools、test）定义在根 POM，模块按需追加。
- 每个模块可独立编译运行，互不影响。
