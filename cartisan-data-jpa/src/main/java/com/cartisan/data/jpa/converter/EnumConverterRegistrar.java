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
 * 为每个枚举类型创建 {@link UniversalEnumConverter} 实例。
 */
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
    private void scanFields(Class<?> clazz, Set<Class<?>> enumTypes) {
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
     */
    public UniversalEnumConverter<?> createConverter(Class<?> enumType) {
        return new UniversalEnumConverter<>(enumType);
    }
}
