package com.cartisan.test.mvc;

/**
 * {@code @CartisanMvcTest} 验证用响应 DTO，携带 BaseEnum 字段。
 *
 * @param orderId 订单号
 * @param status  订单状态
 */
public record MvcOrderResponse(String orderId, MvcOrderStatus status) {
}
