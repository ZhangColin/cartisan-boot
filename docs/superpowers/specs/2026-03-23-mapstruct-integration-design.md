# MapStruct 集成设计文档

**版本**: 1.0
**日期**: 2026-03-23
**状态**: 设计

---

## 一、概述

为 cartisan-boot 框架集成 MapStruct，提供类型安全的对象映射能力，支持领域对象与 DTO/Response 之间的转换。

### 1.1 目标

- 在 `cartisan-dependencies` 中管理 MapStruct 版本
- 在 `cartisan-web` 中提供基础 Mapper 接口
- 提供 Mapper 测试基类
- 编写使用指南文档

### 1.2 技术选型

| 项 | 选择 |
|---|------|
| MapStruct 版本 | 1.6.3 |
| 组件模型 | Spring（支持依赖注入） |
| Lombok 集成 | 是（使用 lombok-mapstruct-binding 0.2.0） |

---

## 二、架构设计

### 2.1 模块依赖

```
cartisan-dependencies
    └── api(mapstruct:1.6.3)           # 版本管理
    └── api(lombok-mapstruct-binding:0.2.0)

cartisan-web
    ├── annotationProcessor(lombok)                    # 1️⃣ 先处理 Lombok
    ├── annotationProcessor(mapstruct-processor)       # 2️⃣ 再处理 MapStruct
    ├── annotationProcessor(lombok-mapstruct-binding)  # 3️⃣ 最后处理集成
    └── DomainMapper                                  # 基础接口
```

**注解处理器顺序说明**：Lombok 必须先于 MapStruct 处理，这样 MapStruct 才能看到 Lombok 生成的代码（如 Builder）。

### 2.2 包结构

```
com.cartisan.web.mapper
├── DomainMapper.java           # 基础 Mapper 接口
└── DomainMapperTest.java       # 测试基类
```

---

## 三、核心组件

### 3.1 DomainMapper 接口

**位置**: `cartisan-web/src/main/java/com/cartisan/web/mapper/DomainMapper.java`

**职责**: 提供默认 Mapper 配置的参考接口

```java
package com.cartisan.web.mapper;

/**
 * MapStruct 基础 Mapper 接口。
 *
 * <p>业务项目的 Mapper 接口继承此接口后，需要添加自己的 {@code @Mapper} 注解，
 * 推荐配置已在注释中说明。
 *
 * <p><b>推荐用法：</b>
 * <pre>{@code
 * @Mapper(  // 继承 DomainMapper 的配置
 *     componentModel = "spring",
 *     nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
 *     nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT
 * )
 * public interface UserMapper extends DomainMapper<User, UserResponse> {
 *     UserResponse toResponse(User user);
 *     List<UserResponse> toResponseList(List<User> users);
 * }
 * }</pre>
 */
public interface DomainMapper<S, T> {
    // 配置参考：业务项目继承后需要添加 @Mapper 注解
    // 推荐配置：
    // - componentModel = "spring": 启用 Spring 依赖注入
    // - nullValueCheckStrategy = ALWAYS: 总是检查 null 值
    // - nullValuePropertyMappingStrategy = SET_TO_DEFAULT: 设置默认值
    // 映射方法由业务项目根据需要定义
}
```

### 3.2 DomainMapperTest 测试基类

**位置**: `cartisan-web/src/test/java/com/cartisan/web/mapper/DomainMapperTest.java`

**职责**: 封装 Mapper 测试的常用断言

```java
package com.cartisan.web.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class DomainMapperTest<S, T, M extends DomainMapper<S, T>> {

    protected abstract M getMapper();

    protected abstract S createSource();

    protected abstract void assertMapped(T target);

    @Test
    void shouldMapSourceToTarget() {
        S source = createSource();
        M mapper = getMapper();

        T target = mapper.toResponse(source);

        assertMapped(target);
    }

    @Test
    void shouldReturnNull_whenSourceIsNull() {
        M mapper = getMapper();

        T target = mapper.toResponse(null);

        assertThat(target).isNull();
    }
}
```

---

## 四、命名约定

| 方法名 | 用途 | 示例 |
|--------|------|------|
| `toXxx()` | 单个对象映射 | `toResponse()`, `toDto()` |
| `toXxxList()` | List 集合映射 | `toResponseList()`, `toDtoList()` |
| `toXxxStream()` | Stream 流映射 | `toResponseStream()`, `toDtoStream()` |

**推荐**: `toResponse()` / `toDto()` 作为主要方法名

---

## 五、业务项目使用示例

### 5.1 定义 Mapper

```java
package com.example.mapper;

import com.cartisan.web.mapper.DomainMapper;
import com.example.domain.User;
import com.example.web.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Stream;

@Mapper  // 继承 DomainMapper 的配置
public interface UserMapper extends DomainMapper<User, UserResponse> {

    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    Stream<UserResponse> toResponseStream(Stream<User> users);
}
```

### 5.2 使用 Mapper

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

### 5.3 测试 Mapper

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

---

## 六、高级用法

### 6.1 字段映射

```java
@Mapper
public interface UserMapper extends DomainMapper<User, UserResponse> {

    @Mapping(source = "fullName", target = "name")
    @Mapping(target = "email", ignore = true)  // 忽略字段
    UserResponse toResponse(User user);
}
```

### 6.2 自定义 null 处理（业务项目按需）

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

### 6.3 Lombok Builder 支持

```java
@Builder
public class UserResponse {
    private Long id;
    private String name;
}

@Mapper(componentModel = "spring")
public interface UserMapper extends DomainMapper<User, UserResponse> {
    // lombok-mapstruct-binding 自动处理 Builder
}
```

---

## 七、实施清单

### 7.1 cartisan-dependencies

- [ ] 添加 `mapstruct:1.6.3` 到 BOM
- [ ] 添加 `lombok-mapstruct-binding:0.2.0` 到 BOM

### 7.2 cartisan-web

- [ ] 添加 `lombok` 到 annotationProcessor（已有，确保顺序）
- [ ] 添加 `mapstruct-processor` 到 annotationProcessor（lombok 之后）
- [ ] 添加 `lombok-mapstruct-binding` 到 annotationProcessor（最后）
- [ ] 创建 `DomainMapper.java`
- [ ] 创建 `DomainMapperTest.java`

### 7.3 文档

- [ ] 创建 `docs/guide/mapstruct-mapping.md`

---

## 八、验证标准

1. 编译通过，MapStruct 生成实现类
2. 测试基类能正常使用
3. Lombok @Builder 生成类能正常映射
4. 文档示例可直接复制使用

---

## 九、参考资源

- MapStruct 官方文档: https://mapstruct.org/
- MapStruct + Lombok 集成: https://mapstruct.org/documentation/latest/reference/html/#lombok
