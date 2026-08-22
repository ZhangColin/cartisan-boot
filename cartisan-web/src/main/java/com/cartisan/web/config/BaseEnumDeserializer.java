package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import com.cartisan.web.exception.InvalidEnumValueException;
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
     * 读取 JSON 值并解析为 Integer code，再使用
     * {@link BaseEnum#parseByCode(Class, Integer)} 查找对应的枚举实例。
     * JSON 值为 null 时返回 null；取值非法（非整数、非数字字符串、未匹配的 code）
     * 时抛出 {@link InvalidEnumValueException}，由全局异常处理器产出携带
     * 字段名与取值表的 400 信封。
     *
     * @param p JSON 解析器
     * @param ctxt 反序列化上下文
     * @return 反序列化后的 BaseEnum 实例，JSON 值为 null 时返回 null
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

        Integer code = readCode(p);
        BaseEnum<?> result = parseByCode(code);
        if (result == null) {
            throw new InvalidEnumValueException(enumType, code);
        }
        return result;
    }

    /**
     * 读取 JSON 值并解析为 Integer code。
     * <p>
     * 接受整数 token 与数字字符串 token（如 {@code "1"}，对齐 Converter 路径的宽容度），
     * 其余 token 视为非法取值。
     */
    private Integer readCode(JsonParser p) throws IOException {
        if (p.getCurrentToken() == com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_INT) {
            return p.getValueAsInt();
        }
        if (p.getCurrentToken() == com.fasterxml.jackson.core.JsonToken.VALUE_STRING) {
            String text = p.getText();
            try {
                return Integer.valueOf(text);
            } catch (NumberFormatException e) {
                throw new InvalidEnumValueException(enumType, text);
            }
        }
        throw new InvalidEnumValueException(enumType, p.getText());
    }

    /**
     * 按目标枚举类型查表解析。
     * <p>
     * {@link BaseEnum#parseByCode} 的类型参数要求 {@code T extends Enum<T> & BaseEnum<T>}，
     * 而本类持有的枚举类型来自运行时反射，只能以 raw type 转换绕开泛型约束。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private BaseEnum<?> parseByCode(Integer code) {
        return parseByCode((Class) enumType, code);
    }

    private <T extends Enum<T> & BaseEnum<T>> BaseEnum<?> parseByCode(Class<T> type, Integer code) {
        return BaseEnum.parseByCode(type, code);
    }
}