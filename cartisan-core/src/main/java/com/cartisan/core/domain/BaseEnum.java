package com.cartisan.core.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 业务枚举基类。
 * <p>
 * 业务枚举实现此接口后，框架自动完成：
 * <ul>
 *   <li>JPA：int ↔ enum 转换（需在枚举内声明 JpaConverter 类）</li>
 *   <li>Jackson：enum ↔ int 序列化</li>
 *   <li>Spring MVC：String → enum 参数绑定（@RequestParam、@PathVariable）</li>
 * </ul>
 *
 * <h2>JPA 持久化示例</h2>
 * <pre>{@code
 * public enum UserStatus implements BaseEnum<UserStatus> {
 *     ACTIVE(1, "激活"),
 *     INACTIVE(0, "未激活");
 *
 *     private final Integer code;
 *     private final String name;
 *
 *     UserStatus(Integer code, String name) {
 *         this.code = code;
 *         this.name = name;
 *     }
 *
 *     @Override
 *     public Integer getCode() { return code; }
 *
 *     @Override
 *     public String getName() { return name; }
 *
 *     // JPA Converter - 必须声明为 public static class
 *     // 注意：类名使用 JpaConverter 而非 Converter，避免与 @Converter 注解冲突
 *     @jakarta.persistence.Converter(autoApply = true)
 *     public static class JpaConverter
 *             extends com.cartisan.data.jpa.converter.BaseEnumConverter<UserStatus> {
 *         public JpaConverter() {
 *             super(UserStatus.class);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>实体类使用时无需任何注解：</p>
 * <pre>{@code
 * @Entity
 * public class User {
 *     @Id
 *     private Long id;
 *
 *     // 无需 @Convert 或 @Enumerated 注解
 *     // UserStatus.JpaConverter 会自动应用
 *     private UserStatus status;
 * }
 * }</pre>
 *
 * <p><b>重要提示：</b>内部类名使用 {@code JpaConverter} 而非 {@code Converter}，
 * 避免与 {@code @Converter} 注解产生命名冲突。</p>
 *
 * @param <T> 枚举类型
 */
public interface BaseEnum<T extends Enum<T> & BaseEnum<T>> {

    /**
     * code → enum 缓存，按 Class 分组，首次访问时懒加载。
     */
    @SuppressWarnings("rawtypes")
    Map<Class<?>, Map<Integer, Object>> CODE_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取编码值（存数据库、传前端）。
     */
    Integer getCode();

    /**
     * 获取名称（显示用）。
     */
    String getName();

    /**
     * 根据 code 解析枚举。
     *
     * @param cls  枚举类型
     * @param code 编码值
     * @return 枚举值，不存在返回 null
     */
    static <T extends Enum<T> & BaseEnum<T>> T parseByCode(Class<T> cls, Integer code) {
        if (code == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<Integer, T> cache = (Map<Integer, T>) (Map<?, ?>) CODE_CACHE.computeIfAbsent(cls, c -> {
            Map<Integer, Object> map = new HashMap<>();
            for (Object e : c.getEnumConstants()) {
                BaseEnum<?> be = (BaseEnum<?>) e;
                map.put(be.getCode(), e);
            }
            return map;
        });
        return cache.get(code);
    }

    /**
     * 根据 code 解析枚举，不存在抛异常。
     *
     * @param cls  枚举类型
     * @param code 编码值
     * @return 枚举值
     * @throws IllegalArgumentException code 无效
     */
    static <T extends Enum<T> & BaseEnum<T>> T requireByCode(Class<T> cls, Integer code) {
        T result = parseByCode(cls, code);
        if (result == null) {
            throw new IllegalArgumentException(
                "Unknown code: " + code + " for " + cls.getSimpleName());
        }
        return result;
    }
}