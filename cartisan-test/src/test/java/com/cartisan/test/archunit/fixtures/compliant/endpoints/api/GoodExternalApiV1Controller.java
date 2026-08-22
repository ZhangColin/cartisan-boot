package com.cartisan.test.archunit.fixtures.compliant.endpoints.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合规的外部 API Controller - 包含版本号 V1，有 @Tag
 */
@Tag(name = "外部商品 API")
@RestController
public class GoodExternalApiV1Controller {
    // Compliant: contains V1 version number
}
