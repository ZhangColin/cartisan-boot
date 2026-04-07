package com.cartisan.data.jpa.converter;

import com.cartisan.data.jpa.annotation.EnumConvert;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 枚举转换器注册器。
 * <p>
 * 扫描 JPA 管理的实体类，收集所有带 {@link EnumConvert} 注解的字段，
 * 为每个枚举类型创建 {@link BaseEnumConverter} 实例。
 *
 * @deprecated 此类将在未来版本中删除，请使用枚举内部的 Converter 类 + @Converter(autoApply = true)
 */
@Deprecated
public class EnumConverterRegistrar {

    private static final Logger log = LoggerFactory.getLogger(EnumConverterRegistrar.class);

    /**
     * 扫描实体类，收集需要转换的枚举类型。
     *
     * @param entityManagerFactory JPA EntityManagerFactory
     * @return 枚举类型去重后的集合
     */
    public Set<Class<?>> scanEnumTypes(EntityManagerFactory entityManagerFactory) {
        Set<Class<?>> enumTypes = new HashSet<>();

        var metamodel = entityManagerFactory.getMetamodel();
        for (EntityType<?> entityType : metamodel.getEntities()) {
            Class<?> clazz = entityType.getJavaType();
            scanFields(clazz, enumTypes);
        }

        return enumTypes;
    }

    /**
     * 扫描类的字段，收集带 @EnumConvert 注解的枚举类型。
     */
    void scanFields(Class<?> clazz, Set<Class<?>> enumTypes) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(EnumConvert.class)) {
                EnumConvert annotation = field.getAnnotation(EnumConvert.class);
                enumTypes.add(annotation.value());
                log.debug("Found @EnumConvert field: {} in {}",
                    field.getName(), clazz.getSimpleName());
            }
        }
    }

    /**
     * 为枚举类型创建 Converter 实例。
     * <p>
     * 注意：此方法将在未来版本中删除，请使用枚举内部的 Converter 类。
     *
     * @deprecated 请使用枚举内部定义的 Converter 类，而不是通过注册器创建
     */
    @Deprecated
    @SuppressWarnings({"rawtypes", "unchecked"})
    public BaseEnumConverter<?> createConverter(Class<?> enumType) {
        return new BaseEnumConverter(enumType) {
            // Anonymous concrete implementation for backward compatibility
        };
    }
}
