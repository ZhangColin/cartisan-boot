# cartisan-boot 文档重构设计方案

> **版本**：v1.0
> **日期**：2026-04-05
> **目标**：重构 cartisan-boot 文档，明确使用手册与编码规范的边界，增强 ArchUnit 规则

---

## 一、背景与目标

### 1.1 当前问题

通过分析 `docs/guide/` 目录下的文档，发现以下问题：

**内容重复**：
- 使用手册与编码规范重复（DDD 设计原则、聚合根设计、枚举处理）
- 内部重复（枚举处理在多个章节重复）

**结构混乱**：
- 章节划分不清晰："模块能力清单"、"核心概念和 API"、"使用示例"、"注意事项"、"详细功能指南"有交叉
- 同一功能在不同章节重复出现

**定位不清**：
- 使用手册包含了编码规范内容
- 使用手册包含了设计文档内容

**内容冗余**：
- 使用示例过于详细（3.1-3.36 共 36 个示例）
- 详细功能指南与使用示例有重复

### 1.2 目标

1. **明确文档定位**：
   - 使用手册：框架使用指南（怎么用）
   - 编码规范：业务代码编写规则（应该怎么写）
   - 设计文档：为什么这样设计

2. **优化文档结构**：便于阅读查询及使用

3. **增强 ArchUnit**：根据编码规范更新架构测试规则

---

## 二、使用手册重构方案

### 2.1 定位

**框架使用指南**：说明 cartisan-boot 框架各模块如何使用，包含设计理念说明。

### 2.2 新结构设计

```
cartisan-boot-使用手册.md
├── 一、快速开始
│   ├── 1.1 依赖引入
│   ├── 1.2 自动配置
│   └── 1.3 基础配置
│
├── 二、模块使用指南
│   ├── 2.1 cartisan-core 模块
│   │   ├── 能力说明
│   │   ├── 核心 API
│   │   ├── 使用示例（精简）
│   │   └── 注意事项（原第四章相关内容）
│   │
│   ├── 2.2 cartisan-test 模块
│   │   ├── 能力说明
│   │   ├── 核心 API
│   │   ├── 使用示例（精简）
│   │   └── 注意事项
│   │
│   ├── 2.3 cartisan-web 模块
│   │   ├── 能力说明
│   │   ├── 核心 API
│   │   ├── 使用示例（精简）
│   │   ├── @Condition 注解详细说明（保留原 5.1）
│   │   └── 注意事项（含 WEB-xxx 规则）
│   │
│   ├── 2.4 cartisan-data-jpa 模块
│   │   ├── 能力说明
│   │   ├── 核心 API
│   │   ├── 使用示例（精简）
│   │   ├── 枚举增强详细说明（保留）
│   │   └── 注意事项（含 DATA-xxx 规则）
│   │
│   ├── 2.5 cartisan-security 模块
│   │   ├── 能力说明
│   │   ├── 核心 API
│   │   ├── 使用示例（精简）
│   │   └── 注意事项（含 SECURITY-xxx 规则）
│   │
│   ├── 2.6 cartisan-data-query 模块
│   │   ├── 能力说明
│   │   ├── 核心 API
│   │   ├── 使用示例（精简）
│   │   └── 注意事项（含 QUERY-xxx 规则）
│   │
│   ├── 2.7 cartisan-ai 模块
│   │   ├── 能力说明
│   │   ├── 核心 API
│   │   ├── 使用示例（精简）
│   │   └── 注意事项（含 AI-xxx 规则）
│   │
│   └── 2.8 cartisan-event 模块
│       ├── 能力说明
│       ├── 核心 API
│       ├── 使用示例
│       └── 注意事项
│
├── 三、设计理念
│   ├── 3.1 六边形架构（端口适配器）
│   ├── 3.2 CQRS 架构（读写分离）
│   └── 3.3 DDD 设计原则（精简版）
│
├── 四、配置说明
│   ├── 4.1 application.yml 配置项
│   ├── 4.2 可选功能开关
│   └── 4.3 Druid 数据源配置
│
├── 五、常见问题
│   └── 关联到 PITFALLS.md
│
└── 六、参考文档
    ├── 设计文档
    ├── 编码规范
    └── AI 协作 SOP
```

### 2.3 示例筛选原则

**保留的示例**（说明框架使用，不重复）：
- 基础类型使用（聚合根、枚举、异常）
- 核心功能使用（Repository、Mapper、权限注解）
- 高级功能使用（jOOQ、AI 调用、流式响应）

**删除/精简的示例**：
- 与编码规范重复的内容（DDD 教条式示例）
- 过于简单的示例（如简单的 getter/setter）
- 重复说明同一功能的多个示例

**预计保留**：15-20 个高质量示例

### 2.4 注意事项处理

**原第四章内容处理方式**：
- 分散到各模块章节中
- 保持规则编号（如 DATA-001、SECURITY-001）
- 只保留与框架直接相关的规则

---

## 三、ArchUnit 规则更新方案

### 3.1 规则来源

1. **限界上下文代码编写规范.md**：业务项目编码规范
2. **cartisan-boot 框架要求**：框架层的架构约束

### 3.2 新增/增强规则

#### 3.2.1 DDD 最佳实践规则（新增）

```java
// 小聚合原则
@ArchTest
static final ArchRule aggregate_should_be_small =
    classes().that().areAssignableTo(AggregateRoot.class)
        .should(new AggregateSizePredicate());

// 聚合根实体引用规则（ID 引用 > 对象引用）
@ArchTest
static final ArchRule aggregate_should_use_id_reference =
    noClasses().that().areAssignableTo(AggregateRoot.class)
        .should().dependOnClassesThat().areAssignableTo(AggregateRoot.class)
        .exceptField("id");

// 应用服务不应暴露领域模型
@ArchTest
static final ArchRule app_service_should_not_expose_domain_model =
    classes().that().areAnnotatedWith(Service.class)
        .and().haveNameMatching(".*AppService")
        .should().onlyDependOnClassesThat()
            .areNotAssignableTo(AggregateRoot.class);
```

#### 3.2.2 分层规则增强

```java
// Controller 不应导入领域模型
@ArchTest
static final ArchRule controller_should_not_import_domain_model =
    noClasses().that().areAnnotatedWith(RestController.class)
        .should().dependOnClassesThat()
            .resideInAPackage("..domain..")
            .exceptAnnotation("*Mapping"); // 允许 @Mapping 注解
```

#### 3.2.3 命名规范增强

```java
// 外部 API Controller 必须包含版本号
@ArchTest
static final ArchRule external_api_controller_must_contain_version =
    classes().that().areAnnotatedWith(RestController.class)
        .and().resideInAPackage("..api..")
        .should().haveNameMatching(".*V\\d+.*");
```

#### 3.2.4 编码规范规则

```java
// 枚举必须实现 BaseEnum
@ArchTest
static final ArchRule enum_should_implement_base_enum =
    classes().that().areEnums()
        .and().resideInAPackage("..domain..")
        .should().implement(BaseEnum.class);

// 常量使用 public static final
@ArchTest
static final ArchRule constants_should_be_public_static_final =
    fields().that().haveNameMatching("[A-Z_]+")
        .should().bePublic()
        .and().shouldBeStatic()
        .and().shouldBeFinal();
```

### 3.3 规则组织

```
CartisanArchRules（总入口）
├── CartisanLayeringRules（分层规则）
│   ├── 现有规则
│   └── 新增：Controller 不应导入领域模型
│
├── CartisanNamingRules（命名规范）
│   ├── 现有规则
│   └── 新增：外部 API Controller 版本号
│
├── CartisanProhibitionRules（禁止规则）
│   ├── 现有规则
│   └── 新增：应用服务不应暴露领域模型
│
└── CartisanDddRules（新增：DDD 最佳实践）
    ├── 聚合根小聚合原则
    ├── ID 引用规则
    ├── 领域服务职责规则
    └── 枚举实现 BaseEnum
```

---

## 四、实施步骤

### 阶段一：文档重构

**目标**：重构 cartisan-boot-使用手册.md，使其成为清晰的框架使用指南

**任务清单**：
1. [ ] 创建新的文档结构
2. [ ] 筛选和精简使用示例（15-20 个）
3. [ ] 将注意事项分散到各模块章节
4. [ ] 保留详细功能指南（@Condition、枚举增强等）
5. [ ] 添加设计理念章节
6. [ ] 审查文档完整性和一致性

**产出物**：
- 重构后的 `docs/guide/cartisan-boot-使用手册.md`

### 阶段二：ArchUnit 更新

**目标**：根据编码规范和框架要求增强 ArchUnit 规则

**任务清单**：
1. [ ] 新增 `CartisanDddRules` 规则类
2. [ ] 实现小聚合检查谓词（`AggregateSizePredicate`）
3. [ ] 增强分层规则（Controller 不应导入领域模型）
4. [ ] 增强命名规范规则（外部 API Controller 版本号）
5. [ ] 新增编码规范规则（枚举、常量）
6. [ ] 编写规则测试用例
7. [ ] 更新 `CartisanArchRules` 总入口

**产出物**：
- 新增 `cartisan-test/src/main/java/.../archunit/CartisanDddRules.java`
- 新增 `cartisan-test/src/main/java/.../archunit/predicate/AggregateSizePredicate.java`
- 修改 `cartisan-test/src/main/java/.../archunit/CartisanLayeringRules.java`
- 修改 `cartisan-test/src/main/java/.../archunit/CartisanNamingRules.java`
- 修改 `cartisan-test/src/main/java/.../archunit/CartisanProhibitionRules.java`
- 修改 `cartisan-test/src/main/java/.../archunit/CartisanArchRules.java`

### 阶段三：验证与调整

**目标**：确保文档重构和 ArchUnit 更新满足需求

**任务清单**：
1. [ ] 检查文档覆盖度（是否遗漏重要功能）
2. [ ] 运行 ArchUnit 测试（cartisan-core 和业务项目）
3. [ ] 根据测试结果调整规则阈值
4. [ ] 完善规则说明文档
5. [ ] 更新使用手册中的 ArchUnit 使用说明

**产出物**：
- 通过测试的 ArchUnit 规则
- 完善的使用手册和规则说明

---

## 五、设计取舍

### 5.1 文档结构

**决策**：选择方案 B（完整版）

**理由**：
- 保留设计理念说明，帮助理解框架设计思路
- 保留详细功能指南，满足深入使用需求
- 保留大部分示例，确保参考价值

**权衡**：
- 文档较长，但结构清晰便于查找
- 内容完整，但需要定期维护更新

### 5.2 示例筛选

**决策**：根据示例作用决定，说明框架使用的、不重复的保留

**理由**：
- 纯框架使用说明的示例保留
- 与编码规范重复的示例删除
- 过于简单或重复的示例精简

**权衡**：
- 示例数量减少，但质量提高
- 查找效率提升，但覆盖度可能下降

### 5.3 注意事项处理

**决策**：将注意事项分散到各模块章节

**理由**：
- 就近原则，阅读时更容易理解
- 避免集中说明导致记忆负担

**权衡**：
- 分散后可能不便于全局查看
- 可以通过规则编号（如 DATA-001）建立索引

### 5.4 ArchUnit 规则增强

**决策**：新增 DDD 最佳实践规则

**理由**：
- 编码规范中包含大量 DDD 最佳实践
- ArchUnit 可以自动验证这些规范
- 帮助业务团队保持代码质量

**权衡**：
- 规则数量增加，复杂度提高
- 需要额外的谓词实现（如小聚合检查）
- 可能需要为业务项目提供豁免机制

---

## 六、成功标准

### 6.1 文档重构成功标准

1. **结构清晰**：
   - 章节划分合理，无交叉重复
   - 目录层次清晰，便于快速查找

2. **定位明确**：
   - 与编码规范无重复内容
   - 聚焦框架使用说明

3. **内容完整**：
   - 覆盖所有模块的核心功能
   - 保留详细功能指南

4. **示例精简**：
   - 示例数量 15-20 个
   - 每个示例都有明确目的

### 6.2 ArchUnit 更新成功标准

1. **规则完整**：
   - 覆盖编码规范中的关键规则
   - 覆盖框架要求的架构约束

2. **规则有效**：
   - 能够发现实际的架构问题
   - 误报率控制在合理范围

3. **组织合理**：
   - 规则分类清晰
   - 便于业务项目选择性使用

---

## 七、风险与缓解

### 7.1 文档重构风险

**风险 1**：精简示例导致覆盖度下降

**缓解措施**：
- 保留最常用的 15-20 个示例
- 在示例之间建立交叉引用
- 提供完整示例仓库链接

**风险 2**：注意事项分散后不便于全局查看

**缓解措施**：
- 保持规则编号（如 DATA-001）
- 在使用手册中提供规则索引
- 在 PITFALLS.md 中保留完整规则列表

### 7.2 ArchUnit 更新风险

**风险 1**：规则过于严格影响开发效率

**缓解措施**：
- 分阶段实施，先观察规则效果
- 为业务项目提供豁免机制
- 允许通过注解禁用特定规则

**风险 2**：谓词实现复杂度高（如小聚合检查）

**缓解措施**：
- 从简单规则开始实施
- 复杂规则使用启发式检查
- 提供规则配置选项

---

## 八、参考文档

- [限界上下文代码编写规范](../../guide/限界上下文代码编写规范.md)
- [cartisan-boot-使用手册](../../guide/cartisan-boot-使用手册.md)
- [cartisan-boot-设计文档](../../cartisan-boot-设计文档.md)
- [PITFALLS.md](../../PITFALLS.md)

---

**文档结束** | **版本**：v1.0 | **日期**：2026-04-05