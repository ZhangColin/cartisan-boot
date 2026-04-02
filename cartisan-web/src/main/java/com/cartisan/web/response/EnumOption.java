package com.cartisan.web.response;

import java.io.Serializable;

/**
 * 枚举选项 DTO，供前端下拉框等组件使用。
 *
 * @param code 枚举 code 值（提交给后端）
 * @param name 枚举显示名称（前端展示）
 * @since 0.9.0
 */
public record EnumOption(
    Integer code,
    String name
) implements Serializable {
}