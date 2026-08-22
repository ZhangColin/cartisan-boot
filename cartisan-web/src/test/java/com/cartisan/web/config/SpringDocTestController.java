package com.cartisan.web.config;

import com.cartisan.web.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * springdoc 集成测试用 Controller。
 *
 * <p>暴露四类枚举形态供 /v3/api-docs 契约断言：
 * BaseEnum 直出字段、普通枚举字段、BaseEnum 集合、BaseEnum 请求参数。</p>
 */
@RestController
@RequestMapping("/springdoc-test")
public class SpringDocTestController {

    /** 未实现 BaseEnum 的普通枚举（schema 必须保持 string+name 不受影响）。 */
    public enum SpringDocCategory {
        PHYSICAL, DIGITAL
    }

    /**
     * 测试响应体：泛型包装 ApiResponse&lt;T&gt; 内嵌的记录。
     */
    public record SpringDocOrderResponse(
            Long id,
            TestUserStatus status,
            SpringDocCategory category,
            List<TestUserStatus> history) {
    }

    @GetMapping("/order")
    public ApiResponse<SpringDocOrderResponse> order() {
        return ApiResponse.ok(new SpringDocOrderResponse(
                1L, TestUserStatus.ACTIVE, SpringDocCategory.DIGITAL, List.of(TestUserStatus.PENDING)));
    }

    @GetMapping("/status")
    public TestUserStatus status(@RequestParam TestUserStatus status) {
        return status;
    }
}
