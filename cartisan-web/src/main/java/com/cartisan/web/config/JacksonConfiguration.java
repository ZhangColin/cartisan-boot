package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.EnumDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局配置。
 *
 * <p>统一 JSON 序列化/反序列化行为：</p>
 * <ul>
 *   <li>Long → 字符串：解决 JavaScript Long 精度问题（JS Number 最大安全整数是 2^53 - 1）</li>
 *   <li>LocalDateTime → ISO 8601：标准日期时间格式</li>
 *   <li>BigDecimal → 禁止科学计数法：保持精度</li>
 *   <li>BaseEnum → code：业务枚举序列化为整数</li>
 *   <li>忽略未知属性：反序列化时忽略未知字段</li>
 * </ul>
 */
@Configuration
public class JacksonConfiguration {

    /**
     * 配置 Jackson2ObjectMapperBuilder。
     *
     * @return Jackson2ObjectMapperBuilderCustomizer 配置器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> builder
            // Long → 字符串（解决 JS 精度问题）
            .serializerByType(Long.class, new ToStringSerializer())
            .serializerByType(Long.TYPE, new ToStringSerializer())

            // LocalDateTime → ISO 8601
            .modules(new JavaTimeModule(), createBaseEnumModule())

            // BigDecimal → 禁用科学计数法
            .featuresToEnable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)

            // BaseEnum → Integer code
            .serializerByType(BaseEnum.class, new BaseEnumSerializer())

            
            // 忽略未知属性
            .featuresToDisable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * 创建 BaseEnum 模块。
     * <p>
     * 使用 BeanDeserializerModifier 拦截所有枚举类型的反序列化，
     * 如果枚举实现了 BaseEnum 接口，则使用 BaseEnumDeserializer。
     *
     * @return SimpleModule 模块
     */
    private SimpleModule createBaseEnumModule() {
        SimpleModule module = new SimpleModule();
        module.setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyEnumDeserializer(DeserializationConfig config,
                                                              JavaType type,
                                                              BeanDescription beanDesc,
                                                              JsonDeserializer<?> deserializer) {
                Class<?> rawClass = type.getRawClass();
                // 如果枚举实现了 BaseEnum 接口，使用 BaseEnumDeserializer
                if (BaseEnum.class.isAssignableFrom(rawClass)) {
                    return new BaseEnumDeserializer(rawClass);
                }
                return deserializer;
            }
        });
        return module;
    }
}
