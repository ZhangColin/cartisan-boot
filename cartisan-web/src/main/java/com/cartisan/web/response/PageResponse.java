package com.cartisan.web.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页响应体。
 *
 * <p>使用 Record 实现，不可变对象。</p>
 *
 * @param <T> 列表项类型
 */
public record PageResponse<T>(
        /** 当前页数据列表 */
        List<T> items,

        /** 总记录数 */
        long total,

        /** 当前页码（从 1 开始） */
        int page,

        /** 每页大小 */
        int size
) {

    /**
     * 从 Spring Data {@link Page} 构造分页响应（1-based 回显）。
     *
     * @param page Spring Data 分页结果
     * @param <T>  列表项类型
     * @return 分页响应，page 为 {@code page.getNumber() + 1}
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getNumber() + 1, page.getSize());
    }
}
