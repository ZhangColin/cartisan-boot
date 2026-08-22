package com.cartisan.test.mvc;

/**
 * {@code @CartisanMvcTest} 验证用请求 DTO，携带 BaseEnum 字段。
 *
 * @param status 订单状态
 */
public record MvcOrderRequest(MvcOrderStatus status) {
}
