package com.cartisan.web.request;

import com.cartisan.core.exception.ApplicationException;
import com.cartisan.core.exception.BaseCodeMessage;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 不分页列表端点的排序参数（如导出全量但客户端控排序）。
 *
 * <p>wire 契约：{@code sort} 为顶级多值参数，每个 token 是属性名或方向关键词
 * （{@code asc}/{@code desc}，大小写不敏感），逗号分隔等价于多值——
 * {@code ?sort=createdAt,desc} 与 {@code ?sort=id,asc&sort=createdAt,desc} 绑定后
 * 归一化为同一扁平 token 流 {@code [id, asc, createdAt, desc]}。方向关键词修饰
 * 紧邻其前的属性，缺省方向 ASC。</p>
 *
 * <p>类型上的 {@code @ParameterObject}（springdoc，optional 依赖）使组件
 * 渲染为 swagger 顶级 query 参数自文档；classpath 无 springdoc 的服务
 * 该注解被静默丢弃，零影响。</p>
 *
 * <p>排序转换全框架唯一实现点：{@link Pagination} 的排序转换内部复用本类型，
 * 不得另建第二套 token 解析。</p>
 *
 * <h2>白名单防御</h2>
 * <p>读侧（jOOQ 等）把属性名字符串拼进 SQL 前，必须经 {@link #toSort(Set)}
 * 白名单校验，白名单外字段抛 {@link ApplicationException}（400 语义，
 * fail loud 而非静默丢弃）。写侧（JPA）可安全使用无白名单的 {@link #toSort()}：
 * 属性名走 criteria 解析，未知属性由 Hibernate 报错。</p>
 *
 * @param sort 排序 token 列表，归一化后非 null（空列表表示无排序）
 * @since 0.2.0
 */
@ParameterObject
public record Ordering(List<String> sort) {

    public Ordering {
        sort = normalize(sort);
    }

    /**
     * 转换为 Spring Data {@link Sort}（无白名单，JPA 写侧适用）。
     *
     * @return Sort，空 token 流返回 {@link Sort#unsorted()}
     */
    public Sort toSort() {
        return parse();
    }

    /**
     * 转换为 Spring Data {@link Sort}，属性名须在白名单内。
     *
     * @param allowedFields 允许的排序属性名集合
     * @return Sort
     * @throws ApplicationException 白名单外字段（400 语义，通用文案）
     */
    public Sort toSort(Set<String> allowedFields) {
        Objects.requireNonNull(allowedFields, "allowedFields must not be null");

        Sort sort = parse();
        for (Sort.Order order : sort) {
            if (!allowedFields.contains(order.getProperty())) {
                throw new ApplicationException(BaseCodeMessage.BAD_REQUEST);
            }
        }
        return sort;
    }

    /**
     * 解析扁平 token 流为有序 Sort：属性名与紧随的方向关键词配对，缺省 ASC。
     */
    private Sort parse() {
        if (sort.isEmpty()) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = new ArrayList<>(sort.size());
        for (int i = 0; i < sort.size(); i++) {
            String token = sort.get(i);
            if (i + 1 < sort.size() && isDirection(sort.get(i + 1))) {
                orders.add(new Sort.Order(Sort.Direction.fromString(sort.get(i + 1)), token));
                i++;
            } else {
                orders.add(Sort.Order.by(token));
            }
        }
        return Sort.by(orders);
    }

    private static boolean isDirection(String token) {
        return Sort.Direction.ASC.name().equalsIgnoreCase(token)
                || Sort.Direction.DESC.name().equalsIgnoreCase(token);
    }

    /**
     * 归一化 token 流：null 元素与空串丢弃、逗号展平、空白裁剪。
     *
     * <p>无论 Spring MVC 把 {@code ?sort=a,desc&sort=b,asc} 绑定成
     * {@code ["a,desc", "b,asc"]} 还是预拆分的 {@code ["a", "desc", "b", "asc"]}，
     * 归一化后 token 流一致，解析行为确定。</p>
     */
    static List<String> normalize(List<String> sort) {
        if (sort == null) {
            return List.of();
        }
        return sort.stream()
                .filter(Objects::nonNull)
                .flatMap(token -> Arrays.stream(token.split(",")))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .toList();
    }
}
