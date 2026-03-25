package com.cartisan.data.jpa.converter;

import com.cartisan.core.domain.BaseEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * 通用枚举转换器。
 * <p>
 * 将实现 {@link BaseEnum} 的枚举类型与数据库 Integer 值相互转换。
 *
 * @param <E> 枚举类型，必须实现 BaseEnum
 */
public class UniversalEnumConverter<E extends Enum<E> & BaseEnum<E>>
        implements AttributeConverter<E, Integer> {

    private final Class<E> enumType;

    /**
     * 创建转换器实例。
     *
     * @param enumType 枚举类型
     */
    @SuppressWarnings("unchecked")
    public UniversalEnumConverter(Class<?> enumType) {
        this.enumType = (Class<E>) enumType;
    }

    @Override
    public Integer convertToDatabaseColumn(E attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public E convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return BaseEnum.requireByCode(enumType, dbData);
    }

    /**
     * 获取此转换器支持的枚举类型。
     */
    public Class<E> getEnumType() {
        return enumType;
    }
}