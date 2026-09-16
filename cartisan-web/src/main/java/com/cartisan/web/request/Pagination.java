package com.cartisan.web.request;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 分页请求参数——框架定义的分页 wire 契约（全链 1-based）。
 *
 * <p>controller 直接声明本类型参数（与其他业务 Query record 并列，无注解、
 * 无嵌套），Spring MVC 按组件名原生绑定顶级参数：</p>
 * <pre>{@code
 * GET /api/admin/users?page=1&size=20&sort=createdAt,desc
 *
 * PageResponse<UserResponse> list(UserQuery query, Pagination pagination) { ... }
 * }</pre>
 *
 * <p>类型上的 {@code @ParameterObject}（springdoc，optional 依赖）使组件
 * 渲染为 swagger 顶级 query 参数自文档；classpath 无 springdoc 的服务
 * 该注解被静默丢弃，零影响。</p>
 *
 * <h2>边界语义（固定契约，非配置）</h2>
 * <ul>
 *   <li>缺省（参数未传）：{@code page=1}、{@code size=20}——组件为 {@code Integer}，
 *       与显式传 0 可区分（显式 0 走 clamp 到 1，而非取默认值）</li>
 *   <li>数值越界静默贴边 clamp：{@code page<1→1}、{@code size<1→1}、{@code size>100→100}</li>
 *   <li>非数值（如 {@code page=abc}）：record 绑定失败走 BindException →
 *       既有 400 field-error 信封</li>
 *   <li>排序 token 语义与 {@link Ordering} 一致（属性名与方向交替，逗号等价多值）</li>
 *   <li>请求页超出尾页：Spring {@code Page} 原生保留请求序号，空 items + 原样回显</li>
 * </ul>
 *
 * <h2>转换出口</h2>
 * <ul>
 *   <li>JPA 写侧：{@link #toPageRequest()}（无白名单，属性名校验交 Hibernate）</li>
 *   <li>白名单排序：{@link #toPageRequest(Set)}（白名单外字段 400）</li>
 *   <li>端点默认排序：{@link #toPageRequest(Sort)}（wire 空 sort 时回退端点级默认）</li>
 *   <li>jOOQ 读侧：{@link #offset()} / {@link #limit()} 直出，配合
 *       {@link Ordering#toSort(Set)} 白名单防注入</li>
 * </ul>
 *
 * <p>分页语义上依赖排序：无 ORDER BY 的 OFFSET/LIMIT 窗口不稳定，
 * 故 sort 并入本类型而非独立参数。不分页但客户端控排序的端点（导出等）
 * 用 {@link Ordering}。</p>
 *
 * @param page 页码（1-based，归一化后非 null 且 ≥1）
 * @param size 每页大小（归一化后非 null，1..100）
 * @param sort 排序 token 列表（归一化后非 null，空列表表示无排序）
 * @since 0.2.0
 */
@ParameterObject
public record Pagination(Integer page, Integer size, List<String> sort) {

    /** 参数缺省时的每页大小（固定契约，非配置项） */
    private static final int DEFAULT_SIZE = 20;

    /** 每页大小上限（固定契约，非配置项） */
    private static final int MAX_SIZE = 100;

    public Pagination {
        page = (page == null || page < 1) ? 1 : page;
        size = (size == null) ? DEFAULT_SIZE : Math.max(1, Math.min(size, MAX_SIZE));
        sort = Ordering.normalize(sort);
    }

    /**
     * 转换为 Spring Data 0-based 分页请求（无排序白名单，JPA 写侧适用）。
     *
     * <p>属性名走 criteria 解析、未知属性由 Hibernate 报错，非注入面。</p>
     *
     * @return PageRequest（页码已转换为 0-based）
     */
    public PageRequest toPageRequest() {
        return PageRequest.of(page - 1, size, toOrdering().toSort());
    }

    /**
     * 转换为 Spring Data 0-based 分页请求，排序属性名须在白名单内。
     *
     * @param allowedFields 允许的排序属性名集合
     * @return PageRequest（页码已转换为 0-based）
     * @throws com.cartisan.core.exception.ApplicationException 白名单外排序字段（400 语义，通用文案）
     */
    public PageRequest toPageRequest(Set<String> allowedFields) {
        return PageRequest.of(page - 1, size, toOrdering().toSort(allowedFields));
    }

    /**
     * 转换为 Spring Data 0-based 分页请求：wire 未传排序时回退端点级默认排序。
     *
     * <p>无 ORDER BY 的 OFFSET/LIMIT 窗口不稳定（分页语义依赖排序），有既定排序
     * 契约的端点在此声明默认（如 {@code Sort.by(Sort.Direction.DESC, "createdAt")}），
     * 替代各端点手写"判空 sort 后三连取值重建 PageRequest"的回退样板（#31）。
     * wire 传了排序时以 wire 为准，{@code defaultSort} 不生效。</p>
     *
     * @param defaultSort 端点默认排序（服务端代码可信，不做白名单校验）
     * @return PageRequest（页码已转换为 0-based）
     */
    public PageRequest toPageRequest(Sort defaultSort) {
        Objects.requireNonNull(defaultSort, "defaultSort must not be null");

        Sort sort = toOrdering().toSort();
        return PageRequest.of(page - 1, size, sort.isSorted() ? sort : defaultSort);
    }

    /**
     * jOOQ 读侧 offset 出口：(page-1)*size。
     *
     * @return offset（long 承载，大页码不溢出）
     */
    public long offset() {
        return (long) (page - 1) * size;
    }

    /**
     * jOOQ 读侧 limit 出口：等于（已 clamp 的）size。
     *
     * @return limit
     */
    public int limit() {
        return size;
    }

    Ordering toOrdering() {
        return new Ordering(sort);
    }
}
