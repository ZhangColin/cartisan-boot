package com.cartisan.data.jpa.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;

/**
 * JPA Specification 查询条件构建器。
 *
 * <p>根据查询条件对象动态生成 JPA Specification，支持 11 种查询类型。</p>
 *
 * <p>支持的查询类型：</p>
 * <ul>
 *   <li>相等性比较：EQUAL, NOT_EQUAL</li>
 *   <li>大小比较：GREATER_EQUAL, GREATER, LESS_EQUAL, LESS</li>
 *   <li>模糊查询：INNER_LIKE, LEFT_LIKE, RIGHT_LIKE</li>
 *   <li>集合与区间查询：IN, BETWEEN</li>
 * </ul>
 *
 * @since 0.3.0
 */
public final class ConditionSpecifications {

    private ConditionSpecifications() {
        // 工具类，禁止实例化
    }

    /**
     * 根据查询条件对象创建 Specification。
     *
     * <p>通过反射获取查询条件对象的字段名、类型和值，
     * 结合 {@link Condition} 注解构建对应的 JPA Predicate。</p>
     *
     * @param queryCondition 查询条件对象，必须包含 fieldName、type、value 字段
     * @param <T> 实体类型
     * @return JPA Specification 对象
     * @throws IllegalArgumentException 如果查询条件对象格式不正确
     */
    @SuppressWarnings("unchecked")
    public static <T> Specification<T> of(Object queryCondition) {
        return (root, query, cb) -> {
            try {
                // 通过反射获取查询条件
                String fieldName = getFieldValue(queryCondition, "fieldName");
                ConditionType type = getFieldValue(queryCondition, "type");
                Object value = getFieldValue(queryCondition, "value");

                if (value == null) {
                    return cb.conjunction();
                }

                Path<Object> path = buildPath(root, fieldName);
                return buildPredicate(path, type, value, cb);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid query condition: " + e.getMessage(), e);
            }
        };
    }

    /**
     * 根据带 @Condition 注解的查询 DTO 创建 Specification。
     *
     * <p>通过反射扫描查询 DTO 的所有字段（包括父类字段），提取带有 @Condition 注解的字段，
     * 构建对应的 JPA Predicate 并用 AND 组合。</p>
     *
     * <p>特性：</p>
     * <ul>
     *   <li>支持递归获取父类字段</li>
     *   <li>自动跳过 null 值和空字符串</li>
     *   <li>支持自定义实体属性名（propName）</li>
     *   <li>多个条件用 AND 连接</li>
     * </ul>
     *
     * @param queryCondition 查询条件对象，字段需标注 @Condition 注解
     * @param <T> 实体类型
     * @return JPA Specification 对象
     */
    public static <T> Specification<T> fromAnnotation(Object queryCondition) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 递归获取所有字段（包括父类）
            List<Field> fields = getAllFields(queryCondition.getClass());

            for (Field field : fields) {
                Condition condition = field.getAnnotation(Condition.class);
                if (condition == null) {
                    continue;
                }

                field.setAccessible(true);
                Object value;
                try {
                    value = field.get(queryCondition);
                } catch (IllegalAccessException e) {
                    continue;
                }

                // 跳过 null 和空字符串
                if (value == null || (value instanceof String str && str.isEmpty())) {
                    continue;
                }

                // 获取实体属性名（优先使用注解中的 propName，否则使用字段名）
                String propName = condition.propName().isEmpty() ? field.getName() : condition.propName();

                Path<Object> path = buildPath(root, propName);
                predicates.add(buildPredicate(path, condition.type(), value, cb));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 根据查询类型和值构建 Predicate。
     *
     * @param path 属性路径
     * @param type 查询类型
     * @param value 查询值
     * @param cb CriteriaBuilder
     * @return Predicate
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate buildPredicate(Path<Object> path, ConditionType type, Object value, CriteriaBuilder cb) {
        return switch (type) {
            // ==================== 相等性比较 ====================
            case EQUAL -> cb.equal(path, value);
            case NOT_EQUAL -> cb.notEqual(path, value);

            // ==================== 大小比较 ====================
            case GREATER_EQUAL -> cb.greaterThanOrEqualTo(path.as(Comparable.class), (Comparable) value);
            case GREATER -> cb.greaterThan(path.as(Comparable.class), (Comparable) value);
            case LESS_EQUAL -> cb.lessThanOrEqualTo(path.as(Comparable.class), (Comparable) value);
            case LESS -> cb.lessThan(path.as(Comparable.class), (Comparable) value);

            // ==================== 模糊查询 ====================
            case INNER_LIKE -> cb.like(path.as(String.class), "%" + value + "%");
            case LEFT_LIKE -> cb.like(path.as(String.class), "%" + value);
            case RIGHT_LIKE -> cb.like(path.as(String.class), value + "%");

            // ==================== 集合与区间查询 ====================
            case IN -> {
                if (value instanceof Collection<?> collection) {
                    yield path.in(collection);
                } else if (value instanceof Object[] array) {
                    yield path.in(array);
                } else {
                    yield cb.disjunction(); // 无效的 IN 条件
                }
            }
            case BETWEEN -> {
                if (value instanceof List<?> list && list.size() == 2) {
                    yield cb.between(path.as(Comparable.class), (Comparable) list.get(0), (Comparable) list.get(1));
                } else if (value instanceof Object[] array && array.length == 2) {
                    yield cb.between(path.as(Comparable.class), (Comparable) array[0], (Comparable) array[1]);
                } else {
                    yield cb.disjunction(); // 无效的 BETWEEN 条件
                }
            }
        };
    }

    /**
     * 构建嵌套属性路径。
     *
     * <p>支持使用 "." 分隔的嵌套属性路径，如 "user.profile.name"。
     * 对于嵌套路径，使用 JPA Criteria API 的 Path 链式调用。</p>
     *
     * @param root 查询根对象
     * @param propertyName 属性名，支持嵌套路径（如 "user.profile.name"）
     * @param <T> 属性类型
     * @return JPA Path 对象
     */
    @SuppressWarnings("unchecked")
    private static <T> Path<T> buildPath(Root<?> root, String propertyName) {
        if (!propertyName.contains(".")) {
            return root.get(propertyName);
        }

        String[] parts = propertyName.split("\\.");
        Path<?> path = root.get(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            path = path.get(parts[i]);
        }

        return (Path<T>) path;
    }

    /**
     * 通过反射获取对象字段值。
     *
     * @param obj 对象
     * @param fieldName 字段名
     * @param <T> 字段类型
     * @return 字段值
     */
    @SuppressWarnings("unchecked")
    private static <T> T getFieldValue(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException("Field '" + fieldName + "' not found in " + obj.getClass().getName(), e);
        }
    }

    /**
     * 递归获取类及其父类的所有字段。
     *
     * @param clazz 类
     * @return 所有字段列表
     */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = clazz;

        while (currentClass != null && currentClass != Object.class) {
            fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }

        return fields;
    }
}
