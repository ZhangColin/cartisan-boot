# MapStruct 集成实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-step. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标:** 为 cartisan-boot 框架集成 MapStruct 1.6.3，提供类型安全的对象映射能力

**架构:**
- `cartisan-dependencies` 管理 MapStruct 版本（BOM）
- `cartisan-web` 提供 `DomainMapper` 基础接口和 `DomainMapperTest` 测试基类
- 注解处理器顺序：Lombok → MapStruct → lombok-mapstruct-binding

**技术栈:** MapStruct 1.6.3, lombok-mapstruct-binding 0.2.0, Spring Boot 3.4.x

---

## 文件结构

```
cartisan-dependencies/
├── build.gradle.kts                    # 添加 mapstruct 和 lombok-mapstruct-binding

cartisan-web/
├── build.gradle.kts                    # 添加 annotationProcessor
├── src/main/java/com/cartisan/web/mapper/
│   └── DomainMapper.java               # 基础 Mapper 接口
├── src/test/java/com/cartisan/web/mapper/
│   ├── DomainMapperTest.java           # 测试基类
│   └── DomainMapperIntegrationTest.java # 集成测试（验证 MapStruct 生成代码）
└── src/test/java/com/cartisan/web/mapper/fixtures/
    └── TestMapper.java                 # 测试用 Mapper

docs/guide/
└── mapstruct-mapping.md                # 使用指南
```

---

## Task 1: 添加 BOM 依赖

**Files:**
- Modify: `cartisan-dependencies/build.gradle.kts`

- [ ] **Step 1: 添加 MapStruct 依赖到 BOM**

编辑 `cartisan-dependencies/build.gradle.kts`，在 `dependencies` 块中添加：

```kotlin
// MapStruct - 类型安全的对象映射
api("org.mapstruct:mapstruct:1.6.3")

// Lombok + MapStruct 集成
api("org.projectlombok:lombok-mapstruct-binding:0.2.0")
```

**插入位置**: 在 `api("org.projectlombok:lombok:1.18.34")` 之后

- [ ] **Step 2: 验证 Gradle 配置**

运行:
```bash
./gradlew :cartisan-dependencies:dependencies --configuration compileClasspath
```

预期输出: 包含 `org.mapstruct:mapstruct:1.6.3` 和 `org.projectlombok:lombok-mapstruct-binding:0.2.0`

- [ ] **Step 3: 提交**

```bash
git add cartisan-dependencies/build.gradle.kts
git commit -m "feat(dependencies): add MapStruct 1.6.3 and lombok-mapstruct-binding 0.2.0"
```

---

## Task 2: 配置 cartisan-web 注解处理器

**Files:**
- Modify: `cartisan-web/build.gradle.kts`

- [ ] **Step 1: 添加 MapStruct 注解处理器**

编辑 `cartisan-web/build.gradle.kts`，在 `dependencies` 块中添加（**注意顺序**，Lombok 之后）：

```kotlin
// MapStruct 注解处理器（必须在 Lombok 之后）
annotationProcessor("org.mapstruct:mapstruct-processor")

// Lombok + MapStruct 集成（必须在 mapstruct-processor 之后）
annotationProcessor("org.projectlombok:lombok-mapstruct-binding")
```

**完整顺序**:
```kotlin
annotationProcessor(platform(project(":cartisan-dependencies")))
annotationProcessor("org.projectlombok:lombok")
annotationProcessor("org.mapstruct:mapstruct-processor")           // 新增
annotationProcessor("org.projectlombok:lombok-mapstruct-binding") // 新增
```

- [ ] **Step 2: 验证注解处理器配置**

运行:
```bash
./gradlew :cartisan-web:compileJava
```

预期: 编译成功（即使没有 Mapper 类也能编译）

- [ ] **Step 3: 提交**

```bash
git add cartisan-web/build.gradle.kts
git commit -m "feat(web): add MapStruct annotation processors"
```

---

## Task 3: 创建 DomainMapper 基础接口

**Files:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/mapper/DomainMapper.java`

- [ ] **Step 1: 创建 mapper 包目录**

运行:
```bash
mkdir -p cartisan-web/src/main/java/com/cartisan/web/mapper
```

- [ ] **Step 2: 创建 DomainMapper 接口**

创建文件 `cartisan-web/src/main/java/com/cartisan/web/mapper/DomainMapper.java`:

```java
package com.cartisan.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct 基础 Mapper 接口。
 *
 * <p>业务项目的 Mapper 接口继承此接口后，只需添加 {@code @Mapper} 注解即可，
 * 推荐配置已在此接口的注解中定义。
 *
 * <p><b>推荐用法：</b>
 * <pre>{@code
 * @Mapper  // 继承 DomainMapper 的配置
 * public interface UserMapper extends DomainMapper<User, UserResponse> {
 *     UserResponse toResponse(User user);
 *     List<UserResponse> toResponseList(List<User> users);
 * }
 * }</pre>
 *
 * @param <S> 源类型
 * @param <T> 目标类型
 */
@Mapper(
    componentModel = "spring",
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT
)
public interface DomainMapper<S, T> {
    // 配置参考：业务项目继承后只需写 @Mapper
    // 映射方法由业务项目根据需要定义
}
```

- [ ] **Step 3: 验证编译**

运行:
```bash
./gradlew :cartisan-web:compileJava
```

预期: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/mapper/DomainMapper.java
git commit -m "feat(web): add DomainMapper base interface"
```

---

## Task 4: 创建测试基类

**Files:**
- Create: `cartisan-web/src/test/java/com/cartisan/web/mapper/DomainMapperTest.java`

- [ ] **Step 1: 创建测试包目录**

运行:
```bash
mkdir -p cartisan-web/src/test/java/com/cartisan/web/mapper
```

- [ ] **Step 2: 创建 DomainMapperTest 测试基类**

创建文件 `cartisan-web/src/test/java/com/cartisan/web/mapper/DomainMapperTest.java`:

```java
package com.cartisan.web.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapper 测试基类。
 *
 * <p>封装 Mapper 测试的通用逻辑，业务项目的 Mapper 测试继承此类后，
 * 只需实现三个抽象方法即可完成基本测试覆盖。
 *
 * @param <S> 源类型
 * @param <T> 目标类型
 * @param <M> Mapper 类型
 */
public abstract class DomainMapperTest<S, T, M extends DomainMapper<S, T>> {

    /**
     * 获取被测试的 Mapper 实例。
     *
     * <p>使用 {@code Mappers.getMapper()} 获取非 Spring 容器管理的实例。
     *
     * @return Mapper 实例
     */
    protected abstract M getMapper();

    /**
     * 创建用于测试的源对象。
     *
     * @return 测试用的源对象
     */
    protected abstract S createSource();

    /**
     * 断言映射结果是否符合预期。
     *
     * @param target 映射后的目标对象
     */
    protected abstract void assertMapped(T target);

    /**
     * 测试正常映射场景。
     */
    @Test
    void shouldMapSourceToTarget() {
        S source = createSource();
        M mapper = getMapper();

        T target = mapper.toResponse(source);

        assertMapped(target);
    }

    /**
     * 测试源对象为 null 时的行为。
     */
    @Test
    void shouldReturnNull_whenSourceIsNull() {
        M mapper = getMapper();

        T target = mapper.toResponse(null);

        assertThat(target).isNull();
    }
}
```

**注意**: 测试基类假设业务项目定义了 `toResponse()` 方法。如果业务项目使用其他方法名，需要重写测试方法。

- [ ] **Step 3: 提交**

```bash
git add cartisan-web/src/test/java/com/cartisan/web/mapper/DomainMapperTest.java
git commit -m "test(web): add DomainMapperTest base class"
```

---

## Task 5: 创建集成测试（验证 MapStruct 生成代码）

**Files:**
- Create: `cartisan-web/src/test/java/com/cartisan/web/mapper/fixtures/SimpleDto.java`
- Create: `cartisan-web/src/test/java/com/cartisan/web/mapper/fixtures/SimpleEntity.java`
- Create: `cartisan-web/src/test/java/com/cartisan/web/mapper/fixtures/TestMapper.java`
- Create: `cartisan-web/src/test/java/com/cartisan/web/mapper/DomainMapperIntegrationTest.java`

- [ ] **Step 1: 创建 fixtures 包目录**

运行:
```bash
mkdir -p cartisan-web/src/test/java/com/cartisan/web/mapper/fixtures
```

- [ ] **Step 2: 创建测试用的 Entity**

创建文件 `cartisan-web/src/test/java/com/cartisan/web/mapper/fixtures/SimpleEntity.java`:

```java
package com.cartisan.web.mapper.fixtures;

/**
 * 测试用的领域对象。
 */
public record SimpleEntity(
    Long id,
    String name,
    String email
) {
}
```

- [ ] **Step 3: 创建测试用的 DTO**

创建文件 `cartisan-web/src/test/java/com/cartisan/web/mapper/fixtures/SimpleDto.java`:

```java
package com.cartisan.web.mapper.fixtures;

/**
 * 测试用的 DTO。
 */
public record SimpleDto(
    Long id,
    String name,
    String email
) {
}
```

- [ ] **Step 4: 创建 Lombok Builder DTO（验证 @Builder 集成）**

创建文件 `cartisan-web/src/test/java/com/cartisan/web/mapper/fixtures/BuilderDto.java`:

```java
package com.cartisan.web.mapper.fixtures;

import lombok.Builder;

/**
 * 测试用的 Lombok Builder DTO。
 * 用于验证 lombok-mapstruct-binding 集成。
 */
@Builder
public class BuilderDto {
    private final Long id;
    private final String name;
    private final String email;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
```

- [ ] **Step 5: 创建测试用 Mapper**

创建文件 `cartisan-web/src/test/java/com/cartisan/web/mapper/fixtures/TestMapper.java`:

```java
package com.cartisan.web.mapper.fixtures;

import com.cartisan.web.mapper.DomainMapper;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 测试用 Mapper，用于验证 MapStruct 生成代码。
 *
 * <p>注意：使用 {@code componentModel = "default"} 而非继承的 {@code "spring"}，
 * 这样 MapStruct 会生成 {@code INSTANCE} 字段，方便测试时直接获取实例。
 */
@Mapper(componentModel = "default")
public interface TestMapper extends DomainMapper<SimpleEntity, SimpleDto> {

    SimpleDto toResponse(SimpleEntity entity);

    List<SimpleDto> toResponseList(List<SimpleEntity> entities);
}
```

- [ ] **Step 5.5: 创建 BuilderMapper（验证 Lombok @Builder 集成）**

在同一文件 `cartisan-web/src/test/java/com/cartisan/web/mapper/fixtures/TestMapper.java` 中添加：

```java
/**
 * 测试用 Mapper，用于验证 Lombok @Builder 集成。
 */
@Mapper(componentModel = "default")
public interface BuilderMapper extends DomainMapper<SimpleEntity, BuilderDto> {

    BuilderDto toBuilderDto(SimpleEntity entity);
}
```

- [ ] **Step 6: 创建集成测试**

创建文件 `cartisan-web/src/test/java/com/cartisan/web/mapper/DomainMapperIntegrationTest.java`:

```java
package com.cartisan.web.mapper;

import com.cartisan.web.mapper.fixtures.BuilderDto;
import com.cartisan.web.mapper.fixtures.BuilderMapper;
import com.cartisan.web.mapper.fixtures.SimpleDto;
import com.cartisan.web.mapper.fixtures.SimpleEntity;
import com.cartisan.web.mapper.fixtures.TestMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DomainMapper 集成测试")
class DomainMapperIntegrationTest {

    @Test
    @DisplayName("应该生成 Mapper 实现类")
    void shouldGenerateMapperImplementation() {
        TestMapper mapper = TestMapper.INSTANCE;

        SimpleEntity entity = new SimpleEntity(1L, "张三", "zhang@example.com");
        SimpleDto dto = mapper.toResponse(entity);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("张三");
        assertThat(dto.email()).isEqualTo("zhang@example.com");
    }

    @Test
    @DisplayName("应该支持 List 映射")
    void shouldSupportListMapping() {
        TestMapper mapper = TestMapper.INSTANCE;

        List<SimpleEntity> entities = List.of(
            new SimpleEntity(1L, "张三", "zhang@example.com"),
            new SimpleEntity(2L, "李四", "li@example.com")
        );

        List<SimpleDto> dtos = mapper.toResponseList(entities);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).name()).isEqualTo("张三");
        assertThat(dtos.get(1).name()).isEqualTo("李四");
    }

    @Test
    @DisplayName("应该处理 null 输入")
    void shouldHandleNullInput() {
        TestMapper mapper = TestMapper.INSTANCE;

        SimpleDto dto = mapper.toResponse(null);

        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("应该支持 Lombok Builder 映射")
    void shouldSupportLombokBuilderMapping() {
        BuilderMapper mapper = BuilderMapper.INSTANCE;

        SimpleEntity entity = new SimpleEntity(1L, "张三", "zhang@example.com");
        BuilderDto dto = mapper.toBuilderDto(entity);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("张三");
        assertThat(dto.getEmail()).isEqualTo("zhang@example.com");
    }
}
```

- [ ] **Step 6: 运行集成测试**

运行:
```bash
./gradlew :cartisan-web:test --tests DomainMapperIntegrationTest
```

预期: BUILD SUCCESSFUL，测试通过

- [ ] **Step 7: 验证生成的实现类**

运行:
```bash
ls -la cartisan-web/build/generated/sources/annotationProcessor/java/main/com/cartisan/web/mapper/fixtures/
```

预期: 存在 `TestMapperImpl.java` 和 `BuilderMapperImpl.java`

- [ ] **Step 9: 提交**

```bash
git add cartisan-web/src/test/java/com/cartisan/web/mapper/
git commit -m "test(web): add DomainMapper integration test with Lombok Builder support"
```

---

## Task 6: 创建使用指南文档

**Files:**
- Create: `docs/guide/mapstruct-mapping.md`

- [ ] **Step 1: 创建使用指南**

创建文件 `docs/guide/mapstruct-mapping.md`:

```markdown
# MapStruct 对象映射使用指南

> **适用范围**：使用 cartisan-boot 框架的 Spring Boot 业务项目

---

## 一、快速上手

### 1.1 引入依赖

业务项目已在 cartisan-web 中引入 MapStruct，无需额外配置。

### 1.2 定义 Mapper

```java
package com.example.mapper;

import com.cartisan.web.mapper.DomainMapper;
import com.example.domain.User;
import com.example.web.response.UserResponse;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.stream.Stream;

@Mapper  // 继承 DomainMapper 的配置
public interface UserMapper extends DomainMapper<User, UserResponse> {

    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    Stream<UserResponse> toResponseStream(Stream<User> users);
}
```

### 1.3 使用 Mapper

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id);
        return userMapper.toResponse(user);
    }

    public List<UserResponse> listUsers() {
        List<User> users = userRepository.findAll();
        return userMapper.toResponseList(users);
    }
}
```

---

## 二、常用注解

### 2.1 @Mapping

用于字段名不一致或需要特殊处理的场景：

```java
@Mapper
public interface UserMapper extends DomainMapper<User, UserResponse> {

    @Mapping(source = "fullName", target = "name")
    @Mapping(target = "email", ignore = true)  // 忽略字段
    UserResponse toResponse(User user);
}
```

### 2.2 qualifiedByName

使用自定义方法处理字段：

```java
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper
public interface UserMapper extends DomainMapper<User, UserResponse> {

    @Named("nullableToEmpty")
    default String nullableToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Mapping(target = "email", qualifiedByName = "nullableToEmpty")
    UserResponse toResponse(User user);
}
```

---

## 三、命名约定

| 方法名 | 用途 | 示例 |
|--------|------|------|
| `toXxx()` | 单个对象映射 | `toResponse()`, `toDto()` |
| `toXxxList()` | List 集合映射 | `toResponseList()`, `toDtoList()` |
| `toXxxStream()` | Stream 流映射 | `toResponseStream()`, `toDtoStream()` |

**推荐**: `toResponse()` / `toDto()` 作为主要方法名

---

## 四、测试 Mapper

### 4.1 使用测试基类

```java
class UserMapperTest extends DomainMapperTest<User, UserResponse, UserMapper> {

    @Override
    protected UserMapper getMapper() {
        return Mappers.getMapper(UserMapper.class);
    }

    @Override
    protected User createSource() {
        return new User(1L, "张三", "zhang@example.com");
    }

    @Override
    protected void assertMapped(UserResponse target) {
        assertThat(target.getId()).isEqualTo(1L);
        assertThat(target.getName()).isEqualTo("张三");
        assertThat(target.getEmail()).isEqualTo("zhang@example.com");
    }
}
```

### 4.2 验证生成的实现类

MapStruct 会在 `build/generated/sources/annotationProcessor/` 下生成实现类。

---

## 五、Lombok 集成

框架已配置 `lombok-mapstruct-binding`，支持映射 Lombok `@Builder` 生成的类：

```java
@Builder
public class UserResponse {
    private Long id;
    private String name;
}

@Mapper
public interface UserMapper extends DomainMapper<User, UserResponse> {
    // 自动处理 Builder
}
```

---

## 六、常见问题

| 现象 | 原因 | 处理 |
|------|------|------|
| 编译失败：找不到 Mapper 实现 | 注解处理器未配置 | 检查 `build.gradle.kts` 中的 `annotationProcessor` |
| Builder 映射失败 | lombok-mapstruct-binding 未配置 | 确保依赖顺序正确 |
| Spring 注入失败 | componentModel 配置错误 | 确认 `@Mapper(componentModel = "spring")` |

---

## 参考资源

- [MapStruct 官方文档](https://mapstruct.org/)
- cartisan-boot 设计文档：[MapStruct 集成设计](../specs/2026-03-23-mapstruct-integration-design.md)
```

- [ ] **Step 2: 提交**

```bash
git add docs/guide/mapstruct-mapping.md
git commit -m "docs: add MapStruct mapping usage guide"
```

---

## 验证清单

完成所有任务后，运行以下命令验证：

```bash
# 1. 编译所有模块
./gradlew compileJava

# 2. 运行 cartisan-web 测试
./gradlew :cartisan-web:test

# 3. 完整构建（验证模块间兼容性）
./gradlew build -x test

# 4. 验证生成的实现类
ls -la cartisan-web/build/generated/sources/annotationProcessor/java/main/com/cartisan/web/mapper/fixtures/
```

**预期结果**:
- 所有编译成功
- 所有测试通过
- 完整构建成功（无模块间依赖问题）
- `TestMapperImpl.java` 和 `BuilderMapperImpl.java` 存在

---

## 参考

- 设计文档: [docs/superpowers/specs/2026-03-23-mapstruct-integration-design.md](../specs/2026-03-23-mapstruct-integration-design.md)
- MapStruct 官方文档: https://mapstruct.org/
