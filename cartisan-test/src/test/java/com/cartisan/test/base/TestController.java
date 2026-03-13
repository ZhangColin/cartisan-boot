package com.cartisan.test.base;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试用 Mock Controller。
 *
 * <p>用于 {@link ApiTestAssertions} 单元测试，返回固定格式的 JSON 响应。</p>
 *
 * @since 0.1.0
 */
@RestController
public class TestController {

    /**
     * 返回成功响应（code=200）。
     */
    @GetMapping("/test/success")
    public Map<String, Object> success() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("requestId", "req-123");

        Map<String, Object> data = new HashMap<>();
        data.put("id", 1);
        data.put("name", "Test Order");
        response.put("data", data);

        return response;
    }

    /**
     * 返回未找到响应（code=404）。
     */
    @GetMapping("/test/notFound")
    public Map<String, Object> notFound() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 404);
        response.put("message", "Not Found");
        response.put("requestId", "req-404");
        response.put("data", null);
        return response;
    }

    /**
     * 返回错误请求响应（code=400）。
     */
    @GetMapping("/test/badRequest")
    public Map<String, Object> badRequest() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 400);
        response.put("message", "Bad Request");
        response.put("requestId", "req-400");
        response.put("data", null);
        return response;
    }

    /**
     * 返回禁止访问响应（code=403）。
     */
    @GetMapping("/test/forbidden")
    public Map<String, Object> forbidden() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 403);
        response.put("message", "Forbidden");
        response.put("requestId", "req-403");
        response.put("data", null);
        return response;
    }

    /**
     * 返回自定义错误码响应。
     */
    @GetMapping("/test/error")
    public Map<String, Object> error() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 500);
        response.put("message", "Internal Server Error");
        response.put("requestId", "req-500");
        response.put("data", null);
        return response;
    }

    /**
     * 返回嵌套数据的成功响应。
     */
    @GetMapping("/test/nested")
    public Map<String, Object> nested() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("requestId", "req-nested");

        Map<String, Object> data = new HashMap<>();
        data.put("user", Map.of("id", 1, "name", "John"));
        data.put("orderId", "ORD-001");
        response.put("data", data);

        return response;
    }
}
