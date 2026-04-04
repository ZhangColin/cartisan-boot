package com.cartisan.test.archunit.fixtures.violation.endpoints.api;

import org.springframework.web.bind.annotation.RestController;

/**
 * 违规的外部 API Controller - 缺少版本号
 */
@RestController
public class BadExternalApiController {
    // Violating: missing version number
}
