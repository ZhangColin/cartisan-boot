package com.cartisan.web.enums;

import com.cartisan.core.domain.BaseEnum;
import com.cartisan.web.support.EnumOptionUtils;
import com.cartisan.web.response.EnumOption;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 枚举注册表，维护枚举类名到 Class 的映射。
 *
 * @since 0.9.0
 */
public class EnumRegistry {

    private final Map<String, Class<? extends BaseEnum<?>>> enumClassMap = new ConcurrentHashMap<>();

    /**
     * 注册枚举类。
     *
     * @param name      枚举类名（简单类名）
     * @param enumClass 枚举类
     * @throws IllegalArgumentException 如果类不是枚举
     */
    public void register(String name, Class<? extends BaseEnum<?>> enumClass) {
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException("Class must be an enum: " + enumClass.getName());
        }
        enumClassMap.put(name, enumClass);
    }

    /**
     * 获取枚举 Class。
     *
     * @param name 枚举类名
     * @return 枚举 Class
     * @throws IllegalArgumentException 枚举不存在
     */
    public Class<? extends BaseEnum<?>> getEnumClass(String name) {
        Class<? extends BaseEnum<?>> enumClass = enumClassMap.get(name);
        if (enumClass == null) {
            throw new IllegalArgumentException("Enum not found: " + name);
        }
        return enumClass;
    }

    /**
     * 获取枚举选项列表。
     *
     * @param name 枚举类名
     * @return 选项列表
     */
    public List<EnumOption> getEnumOptions(String name) {
        Class<? extends BaseEnum<?>> enumClass = getEnumClass(name);
        return EnumOptionUtils.fromEnumGeneric(enumClass);
    }

    /**
     * 列出所有已注册的枚举。
     *
     * @return 枚举类名集合
     */
    public List<String> listRegisteredEnums() {
        return enumClassMap.keySet().stream().sorted().toList();
    }
}