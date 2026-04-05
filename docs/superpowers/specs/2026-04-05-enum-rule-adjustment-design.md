# ArchUnit 枚举规则调整设计

**日期**: 2026-04-05
**状态**: 设计中
**负责人**: Claude

## 背景

### 当前问题

现有的 ArchUnit 规则要求所有 `domain` 包下的枚举都必须实现 `BaseEnum` 接口。但 `CodeMessage` 枚举（如 `BaseCodeMessage`）是独立的接口体系，用于异常处理，不应被强制实现 `BaseEnum`。

### 影响范围

- **现有规则**: `domainEnumsShouldImplementBaseEnum` 在 `CartisanCodingStandardsRules` 中
- **受影响枚举**: 所有实现了 `CodeMessage` 接口的枚举，如 `BaseCodeMessage`
- **无实际冲突**: 目前 `BaseCodeMessage` 位于 `cartisan-core` 的 `exception` 包，不在 `domain` 包下，但未来业务项目可能在 `domain` 包中定义自己的 `CodeMessage` 枚举

## 设计方案

### 核心思路

**方案 3（已采用）**: 拆分为两条独立规则

1. **规则 1**: 领域枚举必须实现 `BaseEnum`（排除 `CodeMessage` 枚举）
2. **规则 2**: `CodeMessage` 枚举不应实现 `BaseEnum`（防止混淆）

### 规则详细设计

#### 规则 1: domainEnumsShouldImplementBaseEnum

**位置**: `CartisanCodingStandardsRules.java`

**目的**: 确保需要自动转换的业务值枚举实现 `BaseEnum`

**逻辑**:
- 目标: `domain` 包下的所有枚举
- 排除: 实现了 `CodeMessage` 接口的枚举
- 要求: 必须实现 `BaseEnum` 接口

**实现**:
```java
/**
 * 领域层枚举必须实现 BaseEnum（CodeMessage 枚举除外）
 *
 * <p>确保枚举与 Integer 的自动转换。</p>
 * <p>BaseEnum 接口提供 code/name 映射，是框架枚举处理的基础。</p>
 * <p>CodeMessage 枚举用于异常处理，不需要实现 BaseEnum。</p>
 */
@ArchTest
static final ArchRule domainEnumsShouldImplementBaseEnum =
    classes()
        .that()
        .areEnums()
        .and()
        .resideInAPackage("..domain..")
        .and()
        .doNotImplement("com.cartisan.core.exception.CodeMessage")
        .should()
        .implement("com.cartisan.core.domain.BaseEnum")
        .because("Domain enums must implement BaseEnum for automatic Integer conversion (except CodeMessage enums)")
        .allowEmptyShould(true);
```

#### 规则 2: codeMessageEnumsShouldNotImplementBaseEnum

**位置**: `CartisanCodingStandardsRules.java`

**目的**: 防止 `CodeMessage` 枚举错误地实现 `BaseEnum`，避免两个独立的接口体系混淆

**逻辑**:
- 目标: `domain` 包下实现了 `CodeMessage` 接口的枚举
- 要求: 不能实现 `BaseEnum` 接口

**实现**:
```java
/**
 * CodeMessage 枚举不应实现 BaseEnum
 *
 * <p>避免两个独立的接口体系产生混淆。</p>
 * <p>CodeMessage 用于异常处理（code/message/httpStatus），
 * BaseEnum 用于业务值枚举（code/name）。</p>
 */
@ArchTest
static final ArchRule codeMessageEnumsShouldNotImplementBaseEnum =
    classes()
        .that()
        .areEnums()
        .and()
        .resideInAPackage("..domain..")
        .and()
        .implement("com.cartisan.core.exception.CodeMessage")
        .should()
        .notImplement("com.cartisan.core.domain.BaseEnum")
        .because("CodeMessage enums should not implement BaseEnum to avoid confusion")
        .allowEmptyShould(true);
```

### 规则注册

**位置**: `CartisanArchRules.java`

新增规则注册，使业务项目继承 `CartisanArchRules` 后自动获得新规则：

```java
/**
 * CodeMessage 枚举不应实现 BaseEnum
 */
@ArchTest
static final ArchRule codeMessageEnumsShouldNotImplementBaseEnum =
    CartisanCodingStandardsRules.codeMessageEnumsShouldNotImplementBaseEnum;
```

## 设计权衡

### 为什么选择拆分为两条规则？

**优点**:
1. **职责分离**: 两条规则各司其职，逻辑清晰
2. **防止误用**: 主动阻止 `CodeMessage` 枚举实现 `BaseEnum`
3. **易于维护**: 未来调整任一规则不影响另一条
4. **测试友好**: 两条规则可以独立验证

**缺点**:
- 增加了一条规则，但收益大于成本

### 其他被否决的方案

- **方案 1**: 仅排除 `CodeMessage` 枚举，不添加第二条规则
  - 问题: 无法阻止错误的混用
- **方案 2**: 通过 `domain.enums` 包结构分离
  - 问题: 当前代码库中没有 `domain.enums` 包，需要大规模重构

## 接口对比

### BaseEnum

```java
public interface BaseEnum<T extends Enum<T> & BaseEnum<T>> {
    Integer getCode();  // 用于 JPA/Jackson 转换
    String getName();   // 用于显示
}
```

**用途**: 业务值枚举（如用户状态、订单状态）
**自动支持**: JPA 转换、Jackson 序列化、Spring MVC 参数绑定

### CodeMessage

```java
public interface CodeMessage {
    String code();         // 错误码标识
    String message();      // 消息模板（支持占位符）
    int httpStatus();      // HTTP 状态码
}
```

**用途**: 异常处理错误码
**自动支持**: 全局异常处理器识别

**关键区别**: 两个接口体系完全独立，不应混用。

## 测试策略

### 验证点

1. **现有枚举通过**: 确保现有的 `BaseEnum` 实现枚举仍然通过规则
2. **CodeMessage 不违反**: `BaseCodeMessage` 不应触发规则失败（虽然不在 domain 包，但为未来兼容）
3. **业务项目兼容**: 未来业务项目在 `domain` 包中定义的 `CodeMessage` 枚举不受 `BaseEnum` 要求约束

### 运行命令

```bash
# 验证规则修改
mvn test -pl cartisan-test

# 全量测试（如有需要）
mvn test
```

## 实施计划

详见实现计划文档：`docs/superpowers/plans/YYYY-MM-DD-enum-rule-adjustment.md`

## 参考资料

- `CartisanCodingStandardsRules.java` - 编码规范规则定义
- `CartisanArchRules.java` - 规则聚合
- `BaseEnum.java` - 业务枚举接口
- `CodeMessage.java` - 错误码接口
- `BaseCodeMessage.java` - 基础错误码枚举实现
