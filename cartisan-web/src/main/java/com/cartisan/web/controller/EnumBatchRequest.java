package com.cartisan.web.controller;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量获取枚举请求 DTO。
 *
 * @param enums 枚举类名列表
 * @since 0.9.0
 */
public record EnumBatchRequest(
    @NotEmpty List<String> enums
) {}