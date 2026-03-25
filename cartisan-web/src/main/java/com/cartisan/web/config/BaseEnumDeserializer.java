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

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, com.fasterxml.jackson.databind.BeanProperty property) {
        Class<?> targetType = property.getType().getRawClass();
        return new BaseEnumDeserializer(targetType);
    }

    @Override
    public BaseEnum<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Integer code = p.getValueAsInt();
        if (code == null || enumType == null) {
            return null;
        }

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