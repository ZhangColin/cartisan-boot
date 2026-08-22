package com.cartisan.web.config;

import org.springframework.web.bind.annotation.*;

/**
 * BaseEnum 集成测试用 Controller。
 */
@RestController
@RequestMapping("/test/enum")
public class TestEnumController {

    @GetMapping("/request-param")
    public String testRequestParam(@RequestParam TestUserStatus status) {
        return "Status: " + status.name() + " (code=" + status.getCode() + ")";
    }

    @GetMapping("/path-variable/{status}")
    public String testPathVariable(@PathVariable TestUserStatus status) {
        return "Status: " + status.name() + " (code=" + status.getCode() + ")";
    }

    @GetMapping("/optional")
    public String testOptional(@RequestParam(required = false) TestUserStatus status) {
        if (status == null) {
            return "Status is null";
        }
        return "Status: " + status.name() + " (code=" + status.getCode() + ")";
    }

    @PostMapping("/request-body")
    public String testRequestBody(@RequestBody EnumBodyRequest request) {
        return "Status: " + request.status().name() + " (code=" + request.status().getCode() + ")";
    }

    @GetMapping("/long-path/{id}")
    public String testLongPathVariable(@PathVariable Long id) {
        return "Id: " + id;
    }

    /**
     * 含 BaseEnum 字段的请求体。
     */
    record EnumBodyRequest(TestUserStatus status) {}
}