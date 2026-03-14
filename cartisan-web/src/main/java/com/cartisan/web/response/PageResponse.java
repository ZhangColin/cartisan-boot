package com.cartisan.web.response;

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
}
