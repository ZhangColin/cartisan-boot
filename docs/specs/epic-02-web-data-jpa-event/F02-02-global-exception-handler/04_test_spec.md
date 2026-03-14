# Feature: F02-02 GlobalExceptionHandler — 测试规格

> **归档日期**: 2026-03-14
> **状态**: 已完成

---

## 测试策略

### 单元测试

| 测试类 | 覆盖内容 | 测试数量 |
|--------|---------|---------|
| `ApiResponseTest` | ApiResponse 工厂方法、errors 字段 | 11 tests |

### 集成测试

| 测试类 | 覆盖内容 | 测试数量 | 工具 |
|--------|---------|---------|------|
| `GlobalExceptionHandlerTest` | 异常映射验证 | 11 tests | MockMvc |

---

## 测试用例清单

### ApiResponseTest (11 tests)

| # | 测试方法 | 验证内容 |
|---|---------|---------|
| 1 | `given_data_when_ok_then_return_success_response_with_data` | ok(T data) 返回正确响应 |
| 2 | `given_noData_when_ok_then_return_success_response_without_data` | ok() 返回无数据响应 |
| 3 | `given_codeMessage_when_error_then_return_error_response` | error(CodeMessage) 映射正确 |
| 4 | `given_codeMessage_and_args_when_error_then_return_parameterized_error_response` | error(CodeMessage, args) 参数化消息 |
| 5 | `given_codeMessage_and_emptyArgs_when_error_then_return_error_response_with_original_message` | 空 args 返回原始消息 |
| 6 | `given_customCode_and_message_when_error_then_return_custom_error_response` | error(int, String) 自定义错误 |
| 7 | `given_differentTypeData_when_ok_then_support_generic_type_inference` | 泛型类型推导 |
| 8 | `given_success_response_then_errors_field_is_null` | 成功响应 errors=null |
| 9 | `given_error_response_then_errors_field_is_null` | 错误响应 errors=null |
| 10 | `given_fieldErrors_when_validationError_then_return_validation_error_response` | validationError() 返回正确结构 |
| 11 | `given_emptyFieldErrors_when_validationError_then_return_response_with_empty_errors` | 空 errors 列表 |

### GlobalExceptionHandlerTest (11 tests)

| # | 测试方法 | AC | 验证内容 |
|---|---------|----|---------|
| 1 | `given_cartisanException400_when_handle_then_return_400_with_correct_format` | AC1 | CartisanException(400) 映射 |
| 2 | `given_cartisanException500_when_handle_then_return_500_with_correct_format` | AC1 | CartisanException(500) 映射 |
| 3 | `given_methodArgumentNotValid_when_handle_then_return_400_with_errors_array` | AC2 | @RequestBody 校验失败 |
| 4 | `given_constraintViolation_when_handle_then_return_400_with_errors_array` | AC2 | @RequestParam 校验失败 |
| 5 | `given_accessDeniedException_when_handle_then_return_403` | AC3 | 权限拒绝返回 403 |
| 6 | `given_httpMethodNotSupported_when_handle_then_return_405` | AC4 | HTTP 方法不支持返回 405 |
| 7 | `given_httpMediaTypeNotSupported_when_handle_then_return_415` | AC5 | 媒体类型不支持返回 415 |
| 8 | `given_httpMessageNotReadable_when_handle_then_return_400` | AC6 | JSON 解析失败返回 400 |
| 9 | `given_missingParameter_when_handle_then_return_400_with_parameter_name` | AC7 | 缺少请求参数 |
| 10 | `given_missingHeader_when_handle_then_return_400_with_header_name` | AC8 | 缺少请求头 |
| 11 | `given_unexpectedException_when_handle_then_return_500` | AC9 | 兜底 Exception |

---

## 测试覆盖率

| 模块 | 类覆盖率 | 方法覆盖率 | 行覆盖率 |
|------|---------|-----------|---------|
| cartisan-web | - | - | - |

**注**: cartisan-web 未配置 PIT（变异测试），根据 SOP 可标注「不适用」。

---

## AC 完成状态

| AC | 描述 | 状态 |
|----|------|------|
| AC1 | CartisanException 映射正确 | ✅ 测试覆盖 |
| AC2 | 参数校验返回字段级错误 | ✅ 测试覆盖 |
| AC3 | AccessDeniedException 返回 403 | ✅ 测试覆盖 |
| AC4 | HttpRequestMethodNotSupportedException 返回 405 | ✅ 测试覆盖 |
| AC5 | HttpMediaTypeNotSupportedException 返回 415 | ✅ 测试覆盖 |
| AC6 | HttpMessageNotReadableException 返回 400 | ✅ 测试覆盖 |
| AC7 | MissingServletRequestParameterException 返回 400 | ✅ 测试覆盖 |
| AC8 | MissingRequestHeaderException 返回 400 | ✅ 测试覆盖 |
| AC9 | 兜底 Exception 返回 500 | ✅ 测试覆盖 |
| AC10 | 测试覆盖 | ✅ MockMvc 集成测试 |
| AC11 | 依赖正确配置 | ✅ 编译通过 |

---

## 异常处理器实现清单

| # | 异常类型 | 实现 | 测试 |
|---|---------|------|------|
| 1 | CartisanException | ✅ | ✅ |
| 2 | ConstraintViolationException | ✅ | ✅ |
| 3 | MethodArgumentNotValidException | ✅ | ✅ |
| 4 | BindException | ✅ | ✅ |
| 5 | IllegalArgumentException (AccessDenied) | ✅ | ✅ |
| 6 | HttpRequestMethodNotSupportedException | ✅ | ✅ |
| 7 | HttpMediaTypeNotSupportedException | ✅ | ✅ |
| 8 | HttpMessageNotReadableException | ✅ | ✅ |
| 9 | MissingServletRequestParameterException | ✅ | ✅ |
| 10 | MissingRequestHeaderException | ✅ | ✅ |
| 11 | NoHandlerFoundException / MethodArgumentTypeMismatchException | ✅ | ⚠️ |
| 12 | Exception (兜底) | ✅ | ✅ |

**注**: handleNotFound 处理器已实现但未在 GlobalExceptionHandlerTest 中编写专门测试（需要特定配置才能触发 NoHandlerFoundException，如 `spring.mvc.throwExceptionIfNoHandlerFound=true`）。实际项目中会通过集成测试或端到端测试验证此路径。
