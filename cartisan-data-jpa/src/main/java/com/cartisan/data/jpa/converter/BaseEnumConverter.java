package com.cartisan.data.jpa.converter;

import com.cartisan.core.domain.BaseEnum;
import jakarta.persistence.AttributeConverter;

/**
 * BaseEnum JPA 转换器抽象基类。
 * <p>
 * 每个实现 BaseEnum 的枚举应在内部声明一个静态内部类继承此类，
 * 并标注 {@link jakarta.persistence.Converter @Converter(autoApply = true)}。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * public enum OrderStatus implements BaseEnum<OrderStatus> {
 *     PENDING(1, "待支付"),
 *     PAID(2, "已支付");
 *
 *     private final Integer code;
 *     private final String name;
 *
 *     OrderStatus(Integer code, String name) {
 *         this.code = code;
 *         this.name = name;
 *     }
 *
 *     @Override public Integer getCode() { return code; }
 *     @Override public String getName() { return name; }
 *
 *     @Converter(autoApply = true)
 *     public static class Converter extends BaseEnumConverter<OrderStatus> {
 *         public Converter() {
 *             super(OrderStatus.class);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @param <E> 枚举类型，必须实现 BaseEnum
 */
public abstract class BaseEnumConverter<E extends Enum<E> & BaseEnum<E>>
        implements AttributeConverter<E, Integer> {

    private final Class<E> enumType;

    /**
     * 创建转换器实例。
     *
     * @param enumType 枚举类型
     */
    protected BaseEnumConverter(Class<E> enumType) {
        this.enumType = enumType;
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
     *
     * @return 枚举类型
     */
    protected Class<E> getEnumType() {
        return enumType;
    }
}
