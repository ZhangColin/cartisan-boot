package com.cartisan.web.config;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverters;

import java.lang.reflect.Type;

/**
 * BaseEnum Web 契约的共享支撑（包私有）。
 *
 * <p>收敛序列化/springdoc 两侧共用的契约常量与工具，保证两跳一致性由单点定义而非
 * 字面量巧合：</p>
 * <ul>
 *   <li>{@link #NAME_FIELD_SUFFIX} — 展示名字段后缀（{@code status} → {@code statusName}），
 *       {@link BaseEnumNameSerializerModifier} 与 {@link BaseEnumNameFieldModelConverter} 共用</li>
 *   <li>{@link #rawClassOf(AnnotatedType)} — AnnotatedType 裸类提取，两个 ModelConverter 共用</li>
 *   <li>{@link #deregisterFromGlobalSingletons(ModelConverter)} — springdoc 全局单例的
 *       配对注销（PIT-025），两个 ModelConverter 的 destroy() 共用</li>
 * </ul>
 */
final class BaseEnumContractSupport {

    /**
     * 展示名字段后缀：序列化属性名 + 此后缀 = 展示名字段名（如 {@code statusName}）。
     */
    static final String NAME_FIELD_SUFFIX = "Name";

    private BaseEnumContractSupport() {
    }

    /**
     * 由序列化属性名推导展示名字段名。
     *
     * @param propertyName 序列化属性名（命名策略应用后）
     * @return 展示名字段名
     */
    static String nameFieldOf(String propertyName) {
        return propertyName + NAME_FIELD_SUFFIX;
    }

    /**
     * 提取裸类：属性解析路径上 Type 可能是 Class，也可能是 Jackson 解析形（泛型、数组等）。
     *
     * @param annotatedType springdoc 解析类型（可为 null）
     * @return 裸类；无法提取返回 null
     */
    static Class<?> rawClassOf(AnnotatedType annotatedType) {
        if (annotatedType == null) {
            return null;
        }
        Type type = annotatedType.getType();
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        try {
            return com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance()
                    .constructType(type).getRawClass();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 从 springdoc 的 JVM 级静态单例注销 converter（3.0/3.1 两个变体都执行，
     * 未注册过则为无操作）。springdoc 注册后不清理，上下文关闭时必须配对注销，
     * 见 PITFALLS.md PIT-025。
     *
     * @param converter 要注销的 converter 实例
     */
    static void deregisterFromGlobalSingletons(ModelConverter converter) {
        ModelConverters.getInstance().removeConverter(converter);
        ModelConverters.getInstance(true).removeConverter(converter);
    }
}
