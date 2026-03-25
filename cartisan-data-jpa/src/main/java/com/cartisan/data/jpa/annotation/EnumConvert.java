package com.cartisan.data.jpa.annotation;

import com.cartisan.core.domain.BaseEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识需要枚举转换的字段。
 * <p>
 * 配合 {@link com.cartisan.data.jpa.converter.UniversalEnumConverter} 使用，
 * 自动完成枚举与数据库 int 值的转换。
 *
 * <pre>{@code
 * @Entity
 * public class User {
 *     @EnumConvert(Status.class)
 *     @Column(name = "status")
 *     private Status status;
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumConvert {
    /**
     * 枚举类型，必须实现 {@link BaseEnum}。
     */
    Class<? extends Enum<?>> value();
}