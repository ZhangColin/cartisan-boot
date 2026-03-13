package com.cartisan.test.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * MockMvc API 测试断言辅助类。
 *
 * <p>基于 JsonPath 动态解析响应，不依赖 cartisan-web 模块。</p>
 *
 * <h3>响应断言</h3>
 * <pre>{@code
 * mockMvc.perform(get("/api/orders/1"))
 *     .andExpect(status().isOk())
 *     .andDo(ApiTestAssertions::assertOk)
 *     .andExpect(jsonPath("$.data.id").value(1));
 * }</pre>
 *
 * <h3>请求辅助</h3>
 * <pre>{@code
 * String json = ApiTestAssertions.toJson(new CreateOrderRequest("O001"));
 * mockMvc.perform(post("/api/orders")
 *         .contentType(APPLICATION_JSON)
 *         .content(json)
 *         .with(withToken("test-token"))
 *         .with(withTenantId(1L)));
 * }</pre>
 *
 * @since 0.1.0
 */
public final class ApiTestAssertions {

    /** Authorization Header 名称 */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** 租户 ID Header 名称 */
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";

    /** Bearer Token 前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** JsonPath: 响应码 */
    private static final String CODE_PATH = "$.code";

    /** JsonPath: data 字段前缀 */
    private static final String DATA_PATH_PREFIX = "$.data.";

    /** Jackson ObjectMapper 用于 JSON 序列化 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ApiTestAssertions() {
        // 工具类，禁止实例化
    }

    // ========== 响应断言 ==========

    /**
     * 断言响应为成功状态（HTTP 200 且 code=200）。
     *
     * @return ResultMatcher 可用于 {@code .andExpect()}
     */
    public static ResultMatcher assertOk() {
        return result -> {
            status().isOk().match(result);
            jsonPath(CODE_PATH).value(200).match(result);
        };
    }

    /**
     * 断言响应包含指定的错误码。
     *
     * @param code 期望的错误码
     * @return ResultMatcher 可用于 {@code .andExpect()}
     */
    public static ResultMatcher assertError(int code) {
        return result -> jsonPath(CODE_PATH).value(code).match(result);
    }

    /**
     * 断言响应 data 字段中指定路径的值。
     *
     * @param path JsonPath 表达式（不含 $.data. 前缀）
     * @param value 期望的值
     * @return ResultMatcher 可用于 {@code .andExpect()}
     */
    public static ResultMatcher assertData(String path, Object value) {
        return result -> jsonPath(DATA_PATH_PREFIX + path).value(value).match(result);
    }

    /**
     * 断言响应为未找到状态（code=404）。
     *
     * @return ResultMatcher 可用于 {@code .andExpect()}
     */
    public static ResultMatcher assertNotFound() {
        return assertError(404);
    }

    /**
     * 断言响应为错误请求状态（code=400）。
     *
     * @return ResultMatcher 可用于 {@code .andExpect()}
     */
    public static ResultMatcher assertBadRequest() {
        return assertError(400);
    }

    /**
     * 断言响应为禁止访问状态（code=403）。
     *
     * @return ResultMatcher 可用于 {@code .andExpect()}
     */
    public static ResultMatcher assertForbidden() {
        return assertError(403);
    }

    // ========== 请求辅助 ==========

    /**
     * 将对象序列化为 JSON 字符串。
     *
     * @param body 要序列化的对象
     * @return JSON 字符串
     * @throws JsonProcessingException 序列化失败时抛出
     */
    public static String toJson(Object body) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(body);
    }

    /**
     * 创建添加 Bearer Token 的请求处理器。
     *
     * @param token JWT Token
     * @return RequestPostProcessor 可用于 {@code .with()}
     */
    public static RequestPostProcessor withToken(String token) {
        return request -> {
            request.addHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + token);
            return request;
        };
    }

    /**
     * 创建添加租户 ID 的请求处理器。
     *
     * @param tenantId 租户 ID
     * @return RequestPostProcessor 可用于 {@code .with()}
     */
    public static RequestPostProcessor withTenantId(long tenantId) {
        return request -> {
            request.addHeader(TENANT_ID_HEADER, String.valueOf(tenantId));
            return request;
        };
    }
}
