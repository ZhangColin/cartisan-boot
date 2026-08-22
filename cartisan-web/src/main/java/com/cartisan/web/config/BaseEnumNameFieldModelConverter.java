package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.beans.factory.DisposableBean;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * BaseEnum 展示名（{@code xxxName}）的 springdoc schema 同步 converter。
 *
 * <p>与序列化侧的 {@link BaseEnumNameSerializerModifier} 配对，共用
 * {@code cartisan.web.enum-name-fields.enabled} 开关：Jackson 为 BaseEnum 属性
 * 追加 {@code xxxName} 虚拟属性，本 converter 在 swagger schema 中同步合成同名
 * string 属性（位置紧跟 code 属性之后），保证 JSON 与 schema 两跳不漂移。</p>
 *
 * <p>行为规则（与序列化侧逐一对应）：</p>
 * <ul>
 *   <li>字段名 = 序列化属性名（经应用 ObjectMapper 内省，命名策略/@JsonProperty 生效后）
 *       + {@code "Name"}</li>
 *   <li>仅处理直接声明为 BaseEnum 的属性；集合/数组与普通枚举不同步</li>
 *   <li>schema 中已存在同名字段（手写）时保留手写，不覆盖</li>
 *   <li>解析结果为 {@code $ref} 时，改写 {@code context.getDefinedModels()} 中的
 *       完整模型（springdoc 顶层解析的常态路径）</li>
 * </ul>
 *
 * <p>由 {@code CartisanWebAutoConfiguration} 在 classpath 存在 springdoc 且开关
 * 开启时注册为 Bean，经 ModelConverterRegistrar 接入全局解析链。</p>
 *
 * @since 0.2.0
 * @see BaseEnumNameSerializerModifier
 */
public class BaseEnumNameFieldModelConverter implements ModelConverter, DisposableBean {

    private static final String REF_PREFIX = "#/components/schemas/";

    private final ObjectMapper objectMapper;

    public BaseEnumNameFieldModelConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 上下文关闭时从全局单例移除自身。
     *
     * <p>springdoc 把容器内的 ModelConverter Bean 注册进 JVM 级静态单例
     * {@link ModelConverters} 且不做清理；本 converter 按开关条件装配，
     * 不注销会泄漏到同 JVM 的其他上下文（典型：同一测试套件中开关状态
     * 不同的两个 {@code @SpringBootTest} 互相污染）。3.0/3.1 两个单例变体
     * 都执行移除（未注册过则为无操作）。</p>
     */
    @Override
    public void destroy() {
        BaseEnumContractSupport.deregisterFromGlobalSingletons(this);
    }

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context,
                             Iterator<ModelConverter> chain) {
        Schema<?> schema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        if (schema == null) {
            return null;
        }
        Class<?> rawClass = BaseEnumContractSupport.rawClassOf(type);
        if (rawClass == null || BaseEnum.class.isAssignableFrom(rawClass)) {
            // BaseEnum 自身（integer 改写）归 BaseEnumModelConverter
            return schema;
        }
        Schema<?> propertyBearer = resolvePropertyBearer(schema, context);
        if (propertyBearer != null) {
            appendNameFields(propertyBearer, baseEnumPropertyNamesOf(rawClass));
        }
        return schema;
    }

    /**
     * 属性载体：内联对象 schema 直接取；$ref 时取 context 中已定义的完整模型。
     */
    private static Schema<?> resolvePropertyBearer(Schema<?> schema, ModelConverterContext context) {
        if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            return schema;
        }
        String ref = schema.get$ref();
        if (ref != null && ref.startsWith(REF_PREFIX) && context != null) {
            return context.getDefinedModels().get(ref.substring(REF_PREFIX.length()));
        }
        return null;
    }

    /**
     * 用应用 ObjectMapper 内省出 BaseEnum 直接属性的序列化名集合。
     * 经 Jackson 内省保证命名策略、@JsonProperty、record 访问器与序列化侧一致。
     */
    private Set<String> baseEnumPropertyNamesOf(Class<?> beanClass) {
        JavaType javaType = objectMapper.constructType(beanClass);
        BeanDescription beanDesc = objectMapper.getSerializationConfig().introspect(javaType);
        Set<String> names = new HashSet<>();
        for (BeanPropertyDefinition def : beanDesc.findProperties()) {
            if (!def.couldSerialize() || def.getPrimaryType() == null) {
                continue;
            }
            if (BaseEnum.class.isAssignableFrom(def.getPrimaryType().getRawClass())) {
                names.add(def.getName());
            }
        }
        return names;
    }

    /**
     * 重建属性表：每个 BaseEnum 属性之后插入 {@code xxxName}（已存在则保留手写）。
     * 仅对 schema 中确实存在的 code 属性同步——与 JSON 侧 writer 存在性一一对应。
     */
    private static void appendNameFields(Schema<?> objectSchema, Set<String> baseEnumPropertyNames) {
        Map<String, Schema> properties = objectSchema.getProperties();
        if (properties == null || properties.isEmpty() || baseEnumPropertyNames.isEmpty()) {
            return;
        }
        Map<String, Schema> reordered = new LinkedHashMap<>();
        properties.forEach((name, propertySchema) -> {
            reordered.put(name, propertySchema);
            String nameField = BaseEnumContractSupport.nameFieldOf(name);
            if (baseEnumPropertyNames.contains(name) && !properties.containsKey(nameField)) {
                reordered.put(nameField,
                        new StringSchema().description(name + " 的展示名"));
            }
        });
        @SuppressWarnings({"unchecked", "rawtypes"})
        Map<String, Schema> writable = (Map) reordered;
        objectSchema.setProperties(writable);
    }
}
