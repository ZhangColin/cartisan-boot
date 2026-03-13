# Feature: F01-07 — cartisan-test ArchUnit 规则集

> 版本：v0.1 | 日期：2026-03-13
> 状态：Phase 1 完成

---

## 背景

cartisan-boot 作为 DDD 六边形架构的基础框架，需要为业务项目（如 aieducenter-platform）提供**可执行的架构守护**。

AI 辅助开发时代，代码生成速度快但容易违反架构约束——AI 可能让领域层依赖 Spring、让 Controller 直接调用 Repository、把业务逻辑写在 Controller 里。

传统依赖"代码审查"守护架构的方式有两个问题：
1. **滞后性**——问题在 PR 阶段才发现，修改成本高
2. **不一致性**——审查者的经验和标准不统一

ArchUnit 将架构规则写成代码，在每次 `./gradlew test` 时自动验证，实现"架构宪法"的强制执行。

---

## 目标

1. **提供开箱即用的 DDD 架构规则**——业务项目继承一个类即可获得完整的分层架构守护
2. **规则分类清晰**——分层、命名、禁止三大类规则，职责明确
3. **支持灵活组合**——业务项目可以继承全部规则，或按需选择部分规则
4. **规则本身有测试保护**——每条规则都有正向（合规）和反向（违规）测试

---

## 范围

### 包含（In Scope）

**规则定义**（4 个类）：

| 规则类 | 职责 | 规则数量 |
|--------|------|---------|
| `CartisanLayeringRules` | DDD 分层依赖方向验证 | 4 条 |
| `CartisanNamingRules` | 命名规范验证 | 4 条 |
| `CartisanProhibitionRules` | 禁止使用的反模式 | 3 条 |
| `CartisanArchRules` | 聚合全部规则的入口 | 1 个组合类 |

**DDD 分层规则**（4 条）：
1. 领域层不依赖基础设施层
2. 领域层不依赖 Spring
3. Controller 只依赖应用服务
4. 应用服务不直接操作数据库（JPA/JDBC）

**命名规范规则**（4 条）：
1. `@RestController` 类必须以 `Controller` 结尾
2. `@Service` 类在 `..application..` 包中必须以 `AppService` 结尾
3. `@DomainService` 类必须以 `Service` 结尾
4. `@Repository` 接口必须以 `Repository` 结尾

**禁止规则**（3 条）：
1. 禁止 `@Autowired` 字段注入（强制构造函数注入）
2. 禁止使用 `java.util.Date`（强制 `java.time`）
3. 禁止金额字段使用 `Double`/`Float`（强制 `BigDecimal`）

**规则测试**（3 个测试类 + fixtures）：
- 每个规则类对应一个测试类
- fixtures 包含 `compliant/`（合规示例）和 `violation/`（违规示例）
- 每条规则两个测试：合规代码通过、违规代码失败

### 不包含（Out of Scope）

| 不包含 | 原因 |
|--------|------|
| 规则的开关/配置 | cartisan-boot 是 opinionated framework，规则严格固定 |
| 验证 cartisan-core 自身 | cartisan-core 是库不是分层应用，已有 F01-06 零依赖验证 |
| 非 Spring 项目的支持 | cartisan-test 就是 Spring 测试工具箱 |
| 业务项目的自定义规则 | 业务项目可以继承后追加自己的 `@ArchTest` |

---

## 验收标准（Acceptance Criteria）

### AC1: 规则类结构完整
- [ ] `CartisanLayeringRules` 包含 4 条分层规则
- [ ] `CartisanNamingRules` 包含 4 条命名规则
- [ ] `CartisanProhibitionRules` 包含 3 条禁止规则
- [ ] `CartisanArchRules` 使用 `ArchRules.in()` 组合所有规则

### AC2: 分层规则正确性
- [ ] `domainShouldNotDependOnInfrastructure` 规则能捕获 `domain` → `infrastructure` 的依赖
- [ ] `domainShouldNotDependOnSpring` 规则能捕获 `domain` → `org.springframework..` 的依赖
- [ ] `controllersShouldOnlyDependOnApplication` 规则能捕获 `controller` → `domain` 的直接依赖
- [ ] `applicationShouldNotAccessDatabaseDirectly` 规则能捕获 `application` → JPA/JDBC 的依赖

### AC3: 命名规则正确性
- [ ] `controllersShouldBeSuffixed` 规则能捕获 `@RestController` 类不以 `Controller` 结尾
- [ ] `appServicesShouldBeSuffixed` 规则能捕获 `..application..` 包的 `@Service` 不以 `AppService` 结尾
- [ ] `domainServicesShouldBeSuffixed` 规则能捕获 `@DomainService` 类不以 `Service` 结尾
- [ ] `repositoriesShouldBeSuffixed` 规则能捕获 `@Repository` 不以 `Repository` 结尾

### AC4: 禁止规则正确性
- [ ] `noFieldInjection` 规则能捕获 `@Autowired` 字段
- [ ] `noJavaUtilDate` 规则能捕获对 `java.util.Date` 的依赖
- [ ] `noFloatingPointForMoney` 规则能捕获金额字段（按字段名匹配）使用 `Double`/`Float`

### AC5: 规则测试覆盖
- [ ] 每个规则类有对应的测试类
- [ ] fixtures 包含合规和违规示例代码
- [ ] 每条规则有两个测试：合规通过、违规失败
- [ ] 测试类能验证规则的有效性（故意写反例能被捕获）

### AC6: 业务项目使用姿势
- [ ] 业务项目 `extends CartisanArchRules` 即可获得全部规则
- [ ] 业务项目可以使用 `ArchRules.in()` 选择部分规则
- [ ] 业务项目可以在继承后追加自定义规则

### AC7: 文档完整
- [ ] 每个规则类有清晰的 JavaDoc 说明其用途
- [ ] 每条规则有 `@ArchTest` 注解和描述性名称
- [ ] 规则失败时提供清晰的错误消息

---

## 约束

### 技术约束
- 使用 ArchUnit 1.3.0+
- 规则类直接引用 Spring/JPA 注解类（不用字符串匹配）
- 规则使用 `@ArchTest` 静态字段定义
- 聚合使用 `ArchRules.in()` 组合，不用 Java 继承链

### 架构约束
- cartisan-test 模块依赖 `cartisan-core`
- 规则定义在 `cartisan-test/src/main/java`（作为库发布）
- 规则测试和 fixtures 在 `cartisan-test/src/test/java`

### 质量约束
- 所有规则必须有测试保护
- 测试覆盖率 ≥ 80%
- 规则失败消息必须清晰指向问题

---

## 设计决策（Phase 2 前的预决策）

### 决策 1：规则模式 = 严格模式
**选择**：规则固定，业务项目继承后直接生效，不能覆盖或关闭。

**理由**：
- cartisan-boot 从零开始，不存在"历史包袱"问题
- 如果规则有问题应该修规则，而非加开关
- 老项目的渐进式合规使用 ArchUnit 的 `FreezingArchRule`，不需要框架提供

### 决策 2：规则用途 = 业务项目模板
**选择**：规则为业务项目提供模板，不是验证 cartisan-core 自身。

**理由**：
- cartisan-core 是库（domain/exception/stereotype/util），不是分层应用
- cartisan-core 的零依赖守护已在 F01-06 完成
- 分层规则针对的是 `controller/application/domain/infrastructure` 四层结构

### 决策 3：规则组织 = 分类基类 + ArchRules.in() 组合
**选择**：按职责拆分 3 个规则类，聚合类使用 `ArchRules.in()` 组合。

**理由**：
- 分类清晰，业务项目可以只继承需要的规则组
- `ArchRules.in()` 是 ArchUnit 官方推荐的组合方式
- 比继承链更灵活，支持多种使用姿势

### 决策 4：命名规则 = 固定后缀 + 组合约束
**选择**：后缀固定，且使用"注解 + 位置 + 后缀"的组合约束。

**理由**：
- `*AppService` 后缀解决应用服务 vs 领域服务的区分问题
- 组合约束比单纯后缀匹配更精确
- AI 生成代码时能靠名字推断正确的放置位置

### 决策 5：注解引用 = 直接引用注解类
**选择**：直接引用 `Autowired.class` 等，不用字符串匹配类名。

**理由**：
- cartisan-test 从设计上就是 Spring 测试工具箱
- 类型安全：IDE 能跳转、编译期检查、重构时自动跟随
- 为一个不存在的"非 Spring 使用场景"牺牲类型安全不值得

### 决策 6：规则测试 = fixtures + 一正一反
**选择**：每条规则两个测试（合规通过 + 违规失败），fixtures 提供假代码。

**理由**：
- ArchUnit 规则本身是代码，应该有测试保护
- 只测通过不知道规则是否真有"牙齿"
- 只测失败不知道规则是否误伤合规代码

---

## 依赖

### 前置依赖
- **F01-06**：cartisan-core 模块完整性验证（确保 core 模块已完成）

### 后续依赖
- **F01-08**：cartisan-test — Testcontainers 基类（F01-07 完成后开始）

---

## 下一步

进入 **Phase 2: Design**，产出 `02_interface.md`，定义：
- 规则类的接口设计（公开 API）
- 每条规则的具体实现逻辑
- 测试类的结构
- fixtures 的组织方式
