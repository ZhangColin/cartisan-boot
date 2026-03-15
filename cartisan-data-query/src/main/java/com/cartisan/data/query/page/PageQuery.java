package com.cartisan.data.query.page;

/**
 * 分页查询参数。
 *
 * <p>使用 Record 实现，不可变对象。参数自动校验和修正：
 * <ul>
 *   <li>page &lt; 1 时修正为 1</li>
 *   <li>size &lt; 1 时修正为 20</li>
 *   <li>size &gt; 100 时修正为 100</li>
 * </ul>
 *
 * @param page 当前页码（从 1 开始，构造时自动校验）
 * @param size 每页大小（1-100，构造时自动校验）
 */
public record PageQuery(int page, int size) {

    /**
     * Compact Constructor - 参数校验和修正
     */
    public PageQuery {
        // page 校验：小于 1 时修正为 1
        if (page < 1) {
            page = 1;
        }

        // size 校验：小于 1 时修正为 20，大于 100 时修正为 100
        if (size < 1) {
            size = 20;
        } else if (size > 100) {
            size = 100;
        }
    }

    /**
     * 计算数据库查询的 OFFSET 值。
     *
     * @return OFFSET 值，公式：(page - 1) * size
     */
    public long offset() {
        return (long) (page - 1) * size;
    }

    /**
     * 创建 PageQuery 实例的静态工厂方法。
     *
     * <p>等价于 {@code new PageQuery(page, size)}，提供更好的可读性。
     *
     * @param page 页码
     * @param size 每页大小
     * @return PageQuery 实例
     */
    public static PageQuery of(int page, int size) {
        return new PageQuery(page, size);
    }
}
