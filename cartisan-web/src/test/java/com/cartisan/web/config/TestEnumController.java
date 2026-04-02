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
}