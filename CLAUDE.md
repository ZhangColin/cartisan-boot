# cartisan-boot

业务无关的 Java 技术基础框架，基于 DDD 六边形架构，供业务项目复用。

## 技术栈

- Java 21 / Spring Boot 3.4.x / Maven
- 持久化：Spring Data JPA（写）+ jOOQ（读）
- 安全：Sa-Token（抽象层封装，可替换）
- 测试：JUnit 5 + AssertJ + Mockito + ArchUnit + Testcontainers

## 架构约束

- core 层提供 DDD 基础设施（stereotype 注解基于 Spring @Component）
- 只有聚合根可以拥有 Repository
- 所有金额使用 BigDecimal，禁止浮点数
- 构造函数注入，禁止 @Autowired 字段注入

## 编码规范

- DTO 使用 Java Record，构造函数校验不变量
- 测试命名：given_{条件}_when_{操作}_then_{预期结果}
- 测试使用 AssertJ，禁止无意义断言（如 assertTrue(true)）
- 踩坑经验见 docs/PITFALLS.md，遇到相关问题时查阅

## 常用命令

- 编译：`mvn compile`
- 单元测试（无需 Docker）：`mvn test -pl 模块名`
- 全量测试（需 Docker）：`mvn test`
- 变异测试：`mvn org.pitest:pitest-maven:mutationCoverage -pl cartisan-core`（杀死率 >= 70%）
- 安装到本地仓库：`mvn install`
- 打包：`mvn package`

## Agent skills

### Issue tracker

Issues 与 specs 以本地 markdown 文件存放在 `.scratch/` 下。详见 `docs/agents/issue-tracker.md`。

### Triage labels

Triage 使用五个 canonical 角色标签（needs-triage / needs-info / ready-for-agent / ready-for-human / wontfix）。详见 `docs/agents/triage-labels.md`。

### Domain docs

Single-context 布局：根目录 `CONTEXT.md` + `docs/adr/`。详见 `docs/agents/domain.md`。
