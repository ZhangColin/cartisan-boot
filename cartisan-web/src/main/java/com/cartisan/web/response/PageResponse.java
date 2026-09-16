package com.cartisan.web.response;

import com.cartisan.web.request.Pagination;
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

    /**
     * 空分页响应（空行、总 0、回显请求页码与页大小）。
     *
     * <p>清单端点「无命中」空页出口：检索维度上的无命中（如过滤值换算不到、
     * 过滤值非法等）返回 200 + 空行 + 原样回显页码，语义与消费侧手写空页
     * 字面量一致。</p>
     *
     * @param pagination 请求分页参数（页码/页大小已归一化）
     * @param <T>        列表项类型
     * @return 空分页响应
     */
    public static <T> PageResponse<T> empty(Pagination pagination) {
        return new PageResponse<>(List.of(), 0, pagination.page(), pagination.size());
    }
}
