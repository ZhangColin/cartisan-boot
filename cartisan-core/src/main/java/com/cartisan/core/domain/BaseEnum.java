package com.cartisan.core.domain;

/**
 * 基础枚举接口。
 * <p>
 * 业务枚举实现此接口后，框架自动完成：
 * <ul>
 *   <li>JPA：int ↔ enum 转换</li>
 *   <li>Jackson：enum ↔ int 序列化</li>
 * </ul>
 */
public interface BaseEnum<T extends Enum<T> & BaseEnum<T>> {

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
        for (T t : cls.getEnumConstants()) {
            if (t.getCode().equals(code)) {
                return t;
            }
        }
        return null;
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