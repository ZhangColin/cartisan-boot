package com.cartisan.web.support;

import com.cartisan.core.domain.BaseEnum;
import com.cartisan.web.response.EnumOption;

import java.util.Arrays;
import java.util.List;

/**
 * 枚举选项工具类。
 *
 * @since 0.9.0
 */
public class EnumOptionUtils {

    /**
     * 将枚举类转换为选项列表。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 选项列表
     */
    public static <E extends Enum<E> & BaseEnum<E>> List<EnumOption> fromEnum(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
            .map(e -> new EnumOption(e.getCode(), e.getName()))
            .toList();
    }

    /**
     * 将枚举类转换为选项列表（通配符版本，供 EnumRegistry 使用）。
     *
     * @param enumClass 枚举类
     * @return 选项列表
     */
    @SuppressWarnings("rawtypes")
    public static List<EnumOption> fromEnumGeneric(Class<? extends BaseEnum> enumClass) {
        Object[] enumConstants = enumClass.getEnumConstants();
        return Arrays.stream(enumConstants)
            .map(e -> (BaseEnum) e)
            .map(e -> new EnumOption(e.getCode(), e.getName()))
            .toList();
    }

    /**
     * 将枚举数组转换为选项列表。
     *
     * @param enums 枚举数组
     * @param <E>   枚举类型
     * @return 选项列表
     */
    @SafeVarargs
    public static <E extends Enum<E> & BaseEnum<E>> List<EnumOption> fromEnums(E... enums) {
        return Arrays.stream(enums)
            .map(e -> new EnumOption(e.getCode(), e.getName()))
            .toList();
    }
}