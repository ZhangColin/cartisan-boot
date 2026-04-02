# 枚举选项功能设计文档

> **创建日期**：2026-04-02
> **模块**：cartisan-web
> **版本**：v0.9.0

## 一、背景与目标

### 1.1 背景

前端页面需要展示枚举选项列表（下拉框、单选框等 UI 组件）。当前的 cartisan-boot 框架提供了 `BaseEnum` 接口和序列化支持，但缺少将枚举转换为前端可用选项列表的统一方式。

### 1.2 目标

提供开箱即用的枚举选项支持：
- 通用 DTO：`code` + `name` 基础结构
- 工具类：便捷的枚举转换方法
- 默认 Controller：零配置即可获取枚举选项
- 批量接口：减少前端请求次数
- 可扩展性：支持自定义路径和业务逻辑

### 1.3 核心场景

**场景 1：批量获取枚举选项**
```javascript
// 前端一次性获取多个枚举
POST /api/enums/batch
{"enums": ["UserStatus", "OrderStatus"]}

// 响应
{
  "enums": {
    "UserStatus": [{"code": 1, "name": "激活"}, {"code": 0, "name": "禁用"}],
    "OrderStatus": [{"code": 1, "name": "待支付"}]
  }
}
```

**场景 2：在 Response 中包含选项**
```java
public record UserResponse(
    Long id,
    UserStatus status,
    List<EnumOption> statusOptions  // 下拉框数据
) {}
```

## 二、设计决策

### 2.1 包结构

- `EnumOption` → `com.cartisan.web.response`（与 ApiResponse 等放在一起）
- `EnumOptionUtils` → `com.cartisan.web.utils`（新建 utils 包）
- `EnumRegistry` → `com.cartisan.web.enums`（新建 enums 包）
- `EnumController` → `com.cartisan.web.controller`（新建 controller 包）
- `EnumControllerBase` → `com.cartisan.web.controller`（基类）
- `EnumControllerProperties` → `com.cartisan.web.config`

### 2.2 字段命名

统一使用 `code` + `name`：
- 与 `BaseEnum.getCode()` / `getName()` 保持一致
- 前端可轻松映射到组件库的字段需求（如 `label`/`value`）

### 2.3 扩展性

**当前版本**：只包含 `code` 和 `name` 两个字段

**未来扩展方向**（如需要）：
- 继承 `EnumOption` 添加 `disabled`、`group`、`color` 等字段
- 或创建新的 DTO 如 `ExtendedEnumOption`

### 2.4 批量接口格式

**请求格式**：
```json
POST /api/enums/batch
{
  "enums": ["UserStatus", "OrderStatus"]
}
```

**不采用复杂格式的原因**：
- 满足 90% 的使用场景
- 如需过滤，可在返回后由前端处理
- 保持简单，按需扩展

## 三、核心组件

### 3.1 EnumOption（DTO）

**位置**：`com.cartisan.web.response.EnumOption`

```java
package com.cartisan.web.response;

import java.io.Serializable;

/**
 * 枚举选项 DTO，供前端下拉框等组件使用。
 *
 * @record
 * @param code 枚举 code 值（提交给后端）
 * @param name 枚举显示名称（前端展示）
 * @since 0.9.0
 */
public record EnumOption(
    Integer code,
    String name
) implements Serializable {
}
```

### 3.2 EnumOptionUtils（工具类）

**位置**：`com.cartisan.web.utils.EnumOptionUtils`

```java
package com.cartisan.web.utils;

import com.cartisan.core.domain.BaseEnum;
import com.cartisan.web.response.EnumOption;

import java.util.Arrays;
import java.util.List;

/**
 * 枚举选项工具类。
 *
 * @since 0.9.0
 */
public class EnumOptionUtils {

    /**
     * 将枚举类转换为选项列表。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 选项列表
     */
    public static <E extends Enum<E> & BaseEnum<E>> List<EnumOption> fromEnum(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
            .map(e -> new EnumOption(e.getCode(), e.getName()))
            .toList();
    }

    /**
     * 将枚举数组转换为选项列表。
     *
     * @param enums 枚举数组
     * @param <E>   枚举类型
     * @return 选项列表
     */
    @SafeVarargs
    public static <E extends Enum<E> & BaseEnum<E>> List<EnumOption> fromEnums(E... enums) {
        return Arrays.stream(enums)
            .map(e -> new EnumOption(e.getCode(), e.getName()))
            .toList();
    }
}
```

### 3.3 EnumRegistry（注册表）

**位置**：`com.cartisan.web.enums.EnumRegistry`

```java
package com.cartisan.web.enums;

import com.cartisan.core.domain.BaseEnum;
import com.cartisan.web.response.EnumOption;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 枚举注册表，维护枚举类名到 Class 的映射。
 *
 * @since 0.9.0
 */
@Component
public class EnumRegistry {

    private final Map<String, Class<? extends BaseEnum<?>>>> enumClassMap = new ConcurrentHashMap<>();

    /**
     * 注册枚举类。
     *
     * @param name      枚举类名（简单类名）
     * @param enumClass 枚举类
     */
    public void register(String name, Class<? extends BaseEnum<?>> enumClass) {
        enumClassMap.put(name, enumClass);
    }

    /**
     * 获取枚举 Class。
     *
     * @param name 枚举类名
     * @return 枚举 Class
     * @throws IllegalArgumentException 枚举不存在
     */
    public Class<? extends BaseEnum<?>> getEnumClass(String name) {
        Class<? extends BaseEnum<?>> enumClass = enumClassMap.get(name);
        if (enumClass == null) {
            throw new IllegalArgumentException("Enum not found: " + name);
        }
        return enumClass;
    }

    /**
     * 获取枚举选项列表。
     *
     * @param name 枚举类名
     * @return 选项列表
     */
    @SuppressWarnings("unchecked")
    public List<EnumOption> getEnumOptions(String name) {
        Class<? extends BaseEnum<?>> enumClass = getEnumClass(name);
        // 泛型擦除：需要用原始类型调用
        return EnumOptionUtils.fromEnum((Class<Enum<?>>) enumClass);
    }

    /**
     * 列出所有已注册的枚举。
     *
     * @return 枚举类名集合
     */
    public List<String> listRegisteredEnums() {
        return enumClassMap.keySet().stream().sorted().toList();
    }
}
```

### 3.4 EnumController（默认实现）

**位置**：`com.cartisan.web.controller.EnumController`

```java
package com.cartisan.web.controller;

import com.cartisan.web.enums.EnumRegistry;
import com.cartisan.web.response.ApiResponse;
import com.cartisan.web.response.EnumOption;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 枚举选项 Controller（默认实现）。
 *
 * <p>通过配置项 {@code cartisan.web.enum-controller.enabled} 控制是否启用。
 *
 * @since 0.9.0
 */
@RestController
@RequestMapping("${cartisan.web.enum-controller.path:/api/enums}")
@ConditionalOnProperty(
    prefix = "cartisan.web.enum-controller",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class EnumController extends EnumControllerBase {

    private final EnumRegistry enumRegistry;

    public EnumController(EnumRegistry enumRegistry) {
        this.enumRegistry = enumRegistry;
    }

    @GetMapping("/{enumName}")
    public ApiResponse<Map<String, List<EnumOption>>> getEnum(@PathVariable String enumName) {
        return ApiResponse.ok(Map.of("enums", List.of(enumRegistry.getEnumOptions(enumName))));
    }

    @PostMapping("/batch")
    public ApiResponse<Map<String, List<EnumOption>>> batchEnums(
            @RequestBody EnumBatchRequest request) {
        Map<String, List<EnumOption>> result = super.batchEnums(request.enums());
        return ApiResponse.ok(Map.of("enums", result));
    }
}
```

### 3.5 EnumControllerBase（基类）

**位置**：`com.cartisan.web.controller.EnumControllerBase`

```java
package com.cartisan.web.controller;

import com.cartisan.web.enums.EnumRegistry;
import com.cartisan.web.response.EnumOption;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 枚举选项 Controller 基类。
 *
 * <p>业务项目可继承此类并自定义 URL 路径和业务逻辑。
 *
 * @since 0.9.0
 */
public abstract class EnumControllerBase {

    protected final EnumRegistry enumRegistry;

    protected EnumControllerBase(EnumRegistry enumRegistry) {
        this.enumRegistry = enumRegistry;
    }

    /**
     * 批量获取枚举选项。
     *
     * @param enumNames 枚举类名列表
     * @return key=类名, value=选项列表
     */
    protected Map<String, List<EnumOption>> batchEnums(List<String> enumNames) {
        Map<String, List<EnumOption>> result = new HashMap<>();
        for (String enumName : enumNames) {
            result.put(enumName, enumRegistry.getEnumOptions(enumName));
        }
        return result;
    }
}
```

### 3.6 EnumBatchRequest（请求 DTO）

**位置**：`com.cartisan.web.controller.EnumBatchRequest`

```java
package com.cartisan.web.controller;

import java.util.List;

/**
 * 批量获取枚举请求 DTO。
 *
 * @param enums 枚举类名列表
 * @since 0.9.0
 */
public record EnumBatchRequest(
    List<String> enums
) {}
```

### 3.7 EnumControllerProperties（配置属性）

**位置**：`com.cartisan.web.config.EnumControllerProperties`

```java
package com.cartisan.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 枚举 Controller 配置属性。
 *
 * @since 0.9.0
 */
@ConfigurationProperties(prefix = "cartisan.web.enum-controller")
public class EnumControllerProperties {
    private boolean enabled = true;
    private String path = "/api/enums";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
```

### 3.8 EnumScanner（扫描器）

**位置**：`com.cartisan.web.enums.EnumScanner`

```java
package com.cartisan.web.enums;

import com.cartisan.core.domain.BaseEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import jakarta.annotation.PostConstruct;
import java.util.Set;

/**
 * 枚举扫描器，启动时扫描所有实现 BaseEnum 的枚举。
 *
 * @since 0.9.0
 */
@Component
public class EnumScanner {

    private final EnumRegistry registry;

    @Value("${cartisan.web.enum-controller.scan-packages:}")
    private String[] scanPackages;

    public EnumScanner(EnumRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void scanBaseEnums() {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseEnum.class));

        String[] packagesToScan = scanPackages.length > 0 ? scanPackages : new String[]{"com.cartisan", "com.example"};

        for (String basePackage : packagesToScan) {
            Set<org.springframework.beans.factory.config.BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
            for (org.springframework.beans.factory.config.BeanDefinition candidate : candidates) {
                try {
                    String className = candidate.getBeanClassName();
                    Class<?> clazz = ClassUtils.forName(className, getClass().getClassLoader());

                    if (clazz.isEnum() && BaseEnum.class.isAssignableFrom(clazz)) {
                        @SuppressWarnings("unchecked")
                        Class<? extends BaseEnum<?>> enumClass = (Class<? extends BaseEnum<?>>) clazz;
                        registry.register(clazz.getSimpleName(), enumClass);
                    }
                } catch (ClassNotFoundException e) {
                    // ignore
                }
            }
        }
    }
}
```

## 四、配置说明

### 4.1 默认配置

```yaml
# application.yml
cartisan:
  web:
    enum-controller:
      enabled: true         # 默认启用
      path: /api/enums      # 默认路径
```

### 4.2 自定义路径

```yaml
cartisan:
  web:
    enum-controller:
      path: /api/v1/commons/enums
```

### 4.3 禁用默认实现

```yaml
cartisan:
  web:
    enum-controller:
      enabled: false  # 禁用，由业务项目自定义
```

### 4.4 指定扫描包

```yaml
cartisan:
  web:
    enum-controller:
      scan-packages: com.example.admin,com.example.order
```

## 五、使用示例

### 5.1 零配置使用（默认）

```javascript
// 前端调用
const fetchEnums = async () => {
  const response = await fetch('/api/enums/batch', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ enums: ['UserStatus', 'OrderStatus'] })
  });
  const data = await response.json();
  console.log(data.enums.UserStatus);  // [{code: 1, name: "激活"}, ...]
};
```

### 5.2 自定义 Controller

```java
@RestController
@RequestMapping("/api/v2/dict")
public class DictController extends EnumControllerBase {

    public DictController(EnumRegistry enumRegistry) {
        super(enumRegistry);
    }

    @GetMapping("/{enumName}")
    public ApiResponse<List<EnumOption>> getEnum(@PathVariable String enumName) {
        return ApiResponse.ok(enumRegistry.getEnumOptions(enumName));
    }

    @PostMapping("/batch")
    public ApiResponse<Map<String, List<EnumOption>>> batch(
            @RequestBody EnumBatchRequest request) {
        return ApiResponse.ok(batchEnums(request.enums()));
    }

    @GetMapping("/{enumName}/editable")
    public ApiResponse<List<EnumOption>> getEditableEnums(@PathVariable String enumName) {
        List<EnumOption> all = enumRegistry.getEnumOptions(enumName);
        return ApiResponse.ok(filterEditable(all));
    }
}
```

### 5.3 在 Response 中包含选项

```java
public record UserDetailResponse(
    Long id,
    String username,
    UserStatus status,
    List<EnumOption> statusOptions,
    List<EnumOption> roleOptions
) {}

@Mapper(componentModel = "spring")
public interface UserDetailMapper extends DomainMapper<User, UserDetailResponse> {
    @Mapping(target = "statusOptions",
             expression = "java(EnumOptionUtils.fromEnum(UserStatus.class))")
    @Mapping(target = "roleOptions",
             expression = "java(EnumOptionUtils.fromEnum(Role.class))")
    UserDetailResponse toResponse(User user);
}
```

### 5.4 前端字段映射（Ant Design Vue 示例）

```javascript
// 前端适配层
const toSelectOptions = (enumOptions) => {
  return enumOptions.map(o => ({
    label: o.name,
    value: o.code
  }));
};

// 使用
<el-select v-model="form.status">
  <el-option
    v-for="opt in toSelectOptions(statusOptions)"
    :key="opt.value"
    :label="opt.label"
    :value="opt.value"
  />
</el-select>
```

## 六、错误处理

| 错误场景 | HTTP 状态码 | 错误消息 |
|---------|------------|---------|
| 枚举类不存在 | 404 | `Enum not found: Xxx` |
| 枚举未实现 BaseEnum | 400 | `Enum must implement BaseEnum: Xxx` |
| 请求体为空 | 400 | `Enum list cannot be empty` |

## 七、测试策略

### 7.1 单元测试

- `EnumOptionTest` - DTO 测试
- `EnumOptionUtilsTest` - 工具类方法测试
- `EnumRegistryTest` - 注册表功能测试
- `EnumScannerTest` - 扫描逻辑测试

### 7.2 集成测试

- `EnumControllerTest` - 使用 MockMvc 测试端点
- `EnumControllerIntegrationTest` - 端到端测试

### 7.3 测试覆盖要求

- 单元测试覆盖率 ≥ 80%
- 关键路径（扫描、注册、转换）必须有测试

## 八、升级指南

### 8.1 从旧版本升级

当前版本无旧版本兼容问题（新功能）。

### 8.2 迁移建议

如果已有自定义的枚举选项工具类：
1. 逐步替换为 `EnumOptionUtils`
2. 将自定义 Controller 迁移到继承 `EnumControllerBase`
3. 前端添加字段映射适配层

## 九、未来扩展方向

### 9.1 可能的扩展功能

- **扩展字段**：`disabled`、`group`、`color` 等
- **过滤支持**：在批量请求中支持过滤条件
- **国际化**：根据 `Accept-Language` 返回不同语言的 `name`
- **权限控制**：某些枚举可能需要权限验证

### 9.2 实现建议

当有明确需求时，优先考虑：
1. 继承 `EnumOption` 创建扩展版本
2. 在 `EnumControllerBase` 的子类中添加业务逻辑
3. 避免过度设计，保持简单

## 十、注意事项

### 10.1 性能考虑

- 启动时扫描会增加少量启动时间（通常 < 100ms）
- `EnumRegistry` 使用 `ConcurrentHashMap`，线程安全
- 批量接口可显著减少前端请求次数

### 10.2 安全考虑

- 默认暴露所有 `BaseEnum` 枚举
- 如需隐藏敏感枚举，可通过配置 `scan-packages` 限制扫描范围
- 建议生产环境配置路径规范，避免枚举类名猜测

### 10.3 命名规范

- 枚举类名必须是简单类名（不含包名）
- 前端需要知道枚举类的准确拼写
- 建议业务项目制定枚举命名规范

---

**文档版本**：1.0
**最后更新**：2026-04-02
