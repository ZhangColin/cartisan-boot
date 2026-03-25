package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;

/**
 * BaseEnum Jackson 反序列化器。
 * <p>
 * 将 Integer 值反序列化为对应的 BaseEnum 实例。
 * 使用 {@link ContextualDeserializer} 获取目标字段的实际枚举类型。
 */
public class BaseEnumDeserializer extends JsonDeserializer<BaseEnum<?>>
        implements ContextualDeserializer {

    private Class<? extends BaseEnum<?>> enumType;

    // 无参构造函数，用于 Jackson 创建实例
    public BaseEnumDeserializer() {
    }

    // 带参构造函数，用于 createContextual 返回具体类型的反序列化器
    @SuppressWarnings("unchecked")
    public BaseEnumDeserializer(Class<?> enumType) {
        this.enumType = (Class<? extends BaseEnum<?>>) enumType;
    }

    /**
     * 创建上下文相关的反序列化器。
     * <p>
     * Jackson 在处理具体字段时会调用此方法，获取目标字段的实际枚举类型，
     * 并返回一个新的配置了该类型的反序列化器实例。
     *
     * @param ctxt 反序列化上下文
     * @param property 正在反序列化的 Bean 属性
     * @return 配置了具体枚举类型的反序列化器
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, com.fasterxml.jackson.databind.BeanProperty property) {
        Class<?> targetType = property.getType().getRawClass();
        return new BaseEnumDeserializer(targetType);
    }

    /**
     * 将 JSON 值反序列化为 BaseEnum 实例。
     * <p>
     * 从 JSON 中读取 Integer 值，并使用 {@link BaseEnum#parseByCode(Class, Integer)}
     * 查找对应的枚举实例。如果 JSON 值为 null 或未找到匹配的枚举，则返回 null。
     *
     * @param p JSON 解析器
     * @param ctxt 反序列化上下文
     * @return 反序列化后的 BaseEnum 实例，如果 JSON 值为 null 或未找到匹配项则返回 null
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public BaseEnum<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.getCurrentToken() == com.fasterxml.jackson.core.JsonToken.VALUE_NULL) {
            return null;
        }
        if (enumType == null) {
            return null;
        }

        Integer code = p.getValueAsInt();

        // 使用反射调用 BaseEnum.parseByCode 方法
        try {
            java.lang.reflect.Method method = BaseEnum.class.getDeclaredMethod("parseByCode", Class.class, Integer.class);
            return (BaseEnum<?>) method.invoke(null, enumType, code);
        } catch (Exception e) {
            // 如果反射调用失败，返回 null
            return null;
        }
    }
}