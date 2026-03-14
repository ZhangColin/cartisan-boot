# Feature: F02-01 cartisan-web 响应体 — 实施计划

> **对应需求**: [01_requirement.md](./01_requirement.md)
> **对应接口**: [02_interface.md](./02_interface.md)

---

## 目标复述

本 Feature 实现统一的 HTTP 响应体封装：
1. 在 `cartisan-core` 补充 `BaseCodeMessage.SUCCESS` 枚举值
2. 创建 `cartisan-web` 模块
3. 实现 `ApiResponse<T>` Record，提供 5 个静态工厂方法
4. 实现 `PageResponse<T>` Record
5. 编写单元测试覆盖所有场景

**不包含**：requestId 填充逻辑（F02-03）、JSON 序列化配置（F02-09）、全局异常处理（F02-02）

---

## 变更范围

| 操作 | 模块 | 文件路径 | 说明 |
|------|------|---------|------|
| 修改 | cartisan-core | `src/main/java/com/cartisan/core/exception/BaseCodeMessage.java` | 添加 SUCCESS 枚举值 |
| 新增 | cartisan-web | `build.gradle.kts` | 模块构建配置（依赖 cartisan-core） |
| 新增 | cartisan-web | `src/main/java/com/cartisan/web/response/ApiResponse.java` | 响应体 Record + 5 个工厂方法 |
| 新增 | cartisan-web | `src/main/java/com/cartisan/web/response/PageResponse.java` | 分页响应体 Record |
| 新增 | cartisan-web | `src/test/java/com/cartisan/web/response/ApiResponseTest.java` | ApiResponse 单元测试 |
| 新增 | cartisan-web | `src/test/java/com/cartisan/web/response/PageResponseTest.java` | PageResponse 单元测试 |

---

## 核心流程（伪代码）

```
成功响应:
    ApiResponse.ok(data) → new ApiResponse(200, "Success", data, null)

错误响应:
    ApiResponse.error(codeMessage) → new ApiResponse(httpStatus, message, null, null)
    ApiResponse.error(codeMessage, args) → MessageFormat.format(message, args) 然后构造
    ApiResponse.error(code, message) → new ApiResponse(code, message, null, null)

分页响应:
    new PageResponse(items, total, page, size) → 直接构造
```

---

## 原子任务清单

### Step 1: 补充 BaseCodeMessage.SUCCESS

**目标**: 在 cartisan-core 的 BaseCodeMessage 枚举中添加 SUCCESS 值

**操作**:
1. 打开文件: `cartisan-core/src/main/java/com/cartisan/core/exception/BaseCodeMessage.java`
2. 在枚举开头（`BAD_REQUEST` 之前）添加:
   ```java
   /**
    * 200 OK - 请求成功。
    */
   SUCCESS(200, "success", "Success"),
   ```
3. 编译验证: `./gradlew :cartisan-core:compileJava`
4. 运行现有测试确保无破坏: `./gradlew :cartisan-core:test`

**验证**: 编译通过，现有测试全绿

---

### Step 2: 创建 cartisan-web 模块骨架

**目标**: 创建新模块的基础结构和构建配置

**操作**:
1. 创建目录: `cartisan-web/src/main/java/com/cartisan/web/response`
2. 创建目录: `cartisan-web/src/test/java/com/cartisan/web/response`
3. 创建文件: `cartisan-web/build.gradle.kts`:
   ```kotlin
   plugins {
       java
   }

   dependencies {
       implementation(project(":cartisan-core"))
       testImplementation("org.junit.jupiter:junit-jupiter")
       testImplementation("org.assertj:assertj-core")
   }

   tasks.withType<Test> {
       useJUnitPlatform()
   }
   ```
4. 修改: `settings.gradle.kts`，添加 `include("cartisan-web")`
5. 同步 Gradle: `./gradlew --refresh-dependencies`

**验证**: `./gradlew :cartisan-web:projects` 能识别到 cartisan-web 模块

---

### Step 3: 编写 ApiResponseTest 测试（红灯）

**目标**: 基于验收标准编写测试，此时实现不存在

**文件**: `cartisan-web/src/test/java/com/cartisan/web/response/ApiResponseTest.java`

**测试方法**（按 SKILL.md 的 TEST-002 规则命名 `should_*_when_*` 或 `given_*_when_*_then_*`）:

```java
package com.cartisan.web.response;

import com.cartisan.core.exception.BaseCodeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse 单元测试")
class ApiResponseTest {

    @Test
    @DisplayName("应该构造成功响应 - 带数据")
    void should_construct_success_response_with_data() {
        ApiResponse<String> response = ApiResponse.ok("test data");

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.message()).isEqualTo("Success");
        assertThat(response.data()).isEqualTo("test data");
        assertThat(response.requestId()).isNull();
    }

    @Test
    @DisplayName("应该构造成功响应 - 无数据")
    void should_construct_success_response_without_data() {
        ApiResponse<Void> response = ApiResponse.ok();

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.message()).isEqualTo("Success");
        assertThat(response.data()).isNull();
        assertThat(response.requestId()).isNull();
    }

    @Test
    @DisplayName("应该构造错误响应 - 使用 CodeMessage")
    void should_construct_error_response_with_codeMessage() {
        ApiResponse<Void> response = ApiResponse.error(BaseCodeMessage.NOT_FOUND);

        assertThat(response.code()).isEqualTo(404);
        assertThat(response.message()).isEqualTo("Resource not found");
        assertThat(response.data()).isNull();
        assertThat(response.requestId()).isNull();
    }

    @Test
    @DisplayName("应该构造错误响应 - 参数化消息")
    void should_construct_error_response_with_parameterized_message() {
        ApiResponse<Void> response = ApiResponse.error(BaseCodeMessage.INVALID_PARAMETER, "email");

        assertThat(response.code()).isEqualTo(400);
        assertThat(response.message()).isEqualTo("Invalid parameter: email");
        assertThat(response.data()).isNull();
    }

    @Test
    @DisplayName("应该构造错误响应 - 空参数使用原消息")
    void should_construct_error_response_with_empty_args() {
        ApiResponse<Void> response = ApiResponse.error(BaseCodeMessage.INVALID_PARAMETER);

        assertThat(response.code()).isEqualTo(400);
        assertThat(response.message()).isEqualTo("Invalid parameter: {0}");
    }

    @Test
    @DisplayName("应该构造错误响应 - 自定义码和消息")
    void should_construct_error_response_with_custom_code_and_message() {
        ApiResponse<Void> response = ApiResponse.error(500, "Third party error");

        assertThat(response.code()).isEqualTo(500);
        assertThat(response.message()).isEqualTo("Third party error");
        assertThat(response.data()).isNull();
    }

    @Test
    @DisplayName("应该支持泛型类型推导")
    void should_support_generic_type_inference() {
        ApiResponse<String> stringResponse = ApiResponse.ok("text");
        ApiResponse<Integer> intResponse = ApiResponse.ok(42);

        assertThat(stringResponse.data()).isInstanceOf(String.class);
        assertThat(intResponse.data()).isInstanceOf(Integer.class);
    }
}
```

**验证**: `./gradlew :cartisan-web:compileTestJava` → 编译失败（ApiResponse 类不存在）

---

### Step 4: 实现 ApiResponse（绿灯）

**目标**: 实现 ApiResponse 使测试通过

**文件**: `cartisan-web/src/main/java/com/cartisan/web/response/ApiResponse.java`

```java
package com.cartisan.web.response;

import com.cartisan.core.exception.CodeMessage;
import com.cartisan.core.exception.BaseCodeMessage;

import java.util.Optional;

/**
 * 统一 API 响应体。
 *
 * <p>使用 Record 实现，不可变对象。</p>
 *
 * @param <T> 响应数据类型
 */
public record ApiResponse<T>(
        /** HTTP 状态码/业务错误码 */
        int code,

        /** 响应消息 */
        String message,

        /** 响应数据，成功时为业务数据，错误时为 null */
        T data,

        /** 请求追踪 ID，可选字段（由 F02-03 填充） */
        String requestId
) {

    /**
     * 构造成功响应（带数据）。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return code=200, message="Success", data=传入值, requestId=null
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(
                BaseCodeMessage.SUCCESS.httpStatus(),
                BaseCodeMessage.SUCCESS.message(),
                data,
                null
        );
    }

    /**
     * 构造成功响应（无数据）。
     *
     * @return code=200, message="Success", data=null, requestId=null
     */
    public static ApiResponse<Void> ok() {
        return ok(null);
    }

    /**
     * 构造错误响应（使用错误码枚举）。
     *
     * @param codeMessage 错误码枚举
     * @return code=枚举.httpStatus(), message=枚举.message(), data=null
     */
    public static ApiResponse<Void> error(CodeMessage codeMessage) {
        return new ApiResponse<>(
                codeMessage.httpStatus(),
                codeMessage.message(),
                null,
                null
        );
    }

    /**
     * 构造错误响应（支持参数化消息）。
     *
     * <p>消息格式使用 {@link java.text.MessageFormat#format(String, Object...)}，
     * 支持占位符：{@code {0}}、{@code {1}} 等。</p>
     *
     * @param codeMessage 错误码枚举
     * @param args        消息参数（可为空）
     * @return 格式化后的错误响应
     */
    public static ApiResponse<Void> error(CodeMessage codeMessage, Object... args) {
        String formattedMessage;
        if (args == null || args.length == 0) {
            formattedMessage = codeMessage.message();
        } else {
            formattedMessage = MessageFormat.format(codeMessage.message(), args);
        }
        return new ApiResponse<>(
                codeMessage.httpStatus(),
                formattedMessage,
                null,
                null
        );
    }

    /**
     * 构造错误响应（自定义错误码和消息）。
     *
     * <p>用于第三方异常转换、临时错误等场景。</p>
     *
     * @param code    自定义错误码
     * @param message 自定义错误消息
     * @return 自定义错误响应
     */
    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null, null);
    }
}
```

**验证**:
```bash
./gradlew :cartisan-web:compileJava      # 编译通过
./gradlew :cartisan-web:test             # 测试全绿
```

---

### Step 5: 编写 PageResponseTest 测试（红灯）

**目标**: 编写 PageResponse 的测试

**文件**: `cartisan-web/src/test/java/com/cartisan/web/response/PageResponseTest.java`

```java
package com.cartisan.web.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageResponse 单元测试")
class PageResponseTest {

    @Test
    @DisplayName("应该构造分页响应")
    void should_construct_page_response() {
        PageResponse<String> response = new PageResponse<>(
                java.util.List.of("item1", "item2"),
                100L,
                1,
                10
        );

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0)).isEqualTo("item1");
        assertThat(response.total()).isEqualTo(100L);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
    }

    @Test
    @DisplayName("应该支持空列表")
    void should_support_empty_items() {
        PageResponse<String> response = new PageResponse<>(
                java.util.List.of(),
                0L,
                1,
                10
        );

        assertThat(response.items()).isEmpty();
        assertThat(response.total()).isEqualTo(0L);
    }

    @Test
    @DisplayName("应该支持泛型类型")
    void should_support_generic_types() {
        PageResponse<Integer> intResponse = new PageResponse<>(
                java.util.List.of(1, 2, 3),
                3L,
                1,
                10
        );

        assertThat(intResponse.items().get(0)).isInstanceOf(Integer.class);
    }
}
```

**验证**: `./gradlew :cartisan-web:compileTestJava` → 编译失败（PageResponse 类不存在）

---

### Step 6: 实现 PageResponse（绿灯）

**目标**: 实现 PageResponse 使测试通过

**文件**: `cartisan-web/src/main/java/com/cartisan/web/response/PageResponse.java`

```java
package com.cartisan.web.response;

import java.util.List;

/**
 * 分页响应体。
 *
 * <p>使用 Record 实现，不可变对象。</p>
 *
 * @param <T> 列表项类型
 */
public record PageResponse<T>(
        /** 当前页数据列表 */
        List<T> items,

        /** 总记录数 */
        long total,

        /** 当前页码（从 1 开始） */
        int page,

        /** 每页大小 */
        int size
) {
}
```

**验证**:
```bash
./gradlew :cartisan-web:compileJava      # 编译通过
./gradlew :cartisan-web:test             # 测试全绿
```

---

### Step 7: 全量验证

**目标**: 确保所有代码和测试通过

**操作**:
```bash
./gradlew :cartisan-core:test            # 确保核心模块未受影响
./gradlew :cartisan-web:test             # 新模块测试通过
./gradlew test                           # 全量测试
```

**验证**: 所有测试绿灯

---

### Step 8: 代码提交

**目标**: 提交本次 Feature 的所有变更

**操作**:
```bash
git add cartisan-core/src/main/java/com/cartisan/core/exception/BaseCodeMessage.java
git add cartisan-web/
git add settings.gradle.kts
git commit -m "feat(web): add ApiResponse and PageResponse

- Add BaseCodeMessage.SUCCESS to cartisan-core
- Create cartisan-web module with ApiResponse<T> record
  - ok(T data), ok() factory methods for success
  - error(CodeMessage), error(CodeMessage, args), error(int, String) for errors
- Add PageResponse<T> record for pagination
- Full unit test coverage

Refs: F02-01, docs/specs/epic-02-web-data-jpa-event/F02-01-web-response/"
```

---

## 任务进度跟踪

- [ ] Step 1: 补充 BaseCodeMessage.SUCCESS
- [ ] Step 2: 创建 cartisan-web 模块骨架
- [ ] Step 3: 编写 ApiResponseTest（红灯）
- [ ] Step 4: 实现 ApiResponse（绿灯）
- [ ] Step 5: 编写 PageResponseTest（红灯）
- [ ] Step 6: 实现 PageResponse（绿灯）
- [ ] Step 7: 全量验证
- [ ] Step 8: 代码提交

---

## 注意事项

1. **TDD 流程**: 先写测试（Step 3/5），验证红灯，再写实现（Step 4/6）验证绿灯
2. **不跳步**: 测试和实现必须在不同的 Step 中完成
3. **BaseCodeMessage.SUCCESS**: 注意枚举值位置，应在 `BAD_REQUEST` 之前
4. **MessageFormat**: error(CodeMessage, args) 使用 `MessageFormat.format()`，支持 `{0}` 占位符（与 BaseCodeMessage 一致）
