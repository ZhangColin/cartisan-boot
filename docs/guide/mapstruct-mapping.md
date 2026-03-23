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
- cartisan-boot 设计文档：[MapStruct 集成设计](../superpowers/specs/2026-03-23-mapstruct-integration-design.md)