package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BaseEnum 展示名（{@code xxxName}）虚拟属性的序列化修改器。
 *
 * <p>为每个 BaseEnum 类型的属性在序列化输出中追加一个 {@code <属性名>Name} 字段，
 * 值为枚举的 {@link BaseEnum#getName()} 展示名——业务侧零手写，运行时 JSON 自带展示名。
 * 与 springdoc 侧的 {@code BaseEnumNameFieldModelConverter} 共用
 * {@code cartisan.web.enum-name-fields.enabled} 开关，保证 JSON 与 schema 两跳同步。</p>
 *
 * <p>行为规则：</p>
 * <ul>
 *   <li>{@code xxxName} 紧跟原 code 字段输出（包装原 writer 原地追加）</li>
 *   <li>枚举为 null 时镜像原字段的 null 处理：原字段输出 null 则展示名输出 null；
 *       原字段被 {@code NON_NULL} 抑制则展示名同样抑制</li>
 *   <li>DTO 已手写同名字段（如迁移期的 {@code statusName}）时手写优先，不重复输出</li>
 *   <li>仅处理直接声明为 BaseEnum 的属性；集合/数组属性不追加</li>
 * </ul>
 *
 * <p>已知限制：与 unwrapping（{@code @JsonUnwrapped}）叠加时追加行为丢失
 * （Jackson rename 机制会还原 writer）；反序列化方向不消费 {@code xxxName}
 * （框架全局忽略未知属性，前端回传不报错）。</p>
 *
 * @since 0.2.0
 */
public class BaseEnumNameSerializerModifier extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                     BeanDescription beanDesc,
                                                     List<BeanPropertyWriter> beanProperties) {
        Set<String> declaredNames = new HashSet<>();
        for (BeanPropertyWriter writer : beanProperties) {
            declaredNames.add(writer.getName());
        }

        List<BeanPropertyWriter> result = new ArrayList<>(beanProperties.size());
        for (BeanPropertyWriter writer : beanProperties) {
            result.add(wrapIfBaseEnumProperty(writer, declaredNames));
        }
        return result;
    }

    /**
     * BaseEnum 直接属性且未手写同名字段时，包装为追加展示名的 writer；否则原样返回。
     */
    private static BeanPropertyWriter wrapIfBaseEnumProperty(BeanPropertyWriter writer,
                                                             Set<String> declaredNames) {
        if (!BaseEnum.class.isAssignableFrom(writer.getType().getRawClass())) {
            return writer;
        }
        if (declaredNames.contains(BaseEnumContractSupport.nameFieldOf(writer.getName()))) {
            return writer;
        }
        return new EnumNameAppendingWriter(writer);
    }

    /**
     * 先按原逻辑输出 code 字段、随后追加 {@code xxxName} 字符串字段的 writer。
     *
     * <p>继承 {@link BeanPropertyWriter} 并复用父类全部装配（accessor、序列化器、
     * null 抑制标记），仅扩展 {@link #serializeAsField}；访问器会调用两次
     * （父类输出一次、取展示名一次），对 record/常规 DTO 的幂等访问器无影响。</p>
     */
    static final class EnumNameAppendingWriter extends BeanPropertyWriter {

        private final SerializedString nameField;

        EnumNameAppendingWriter(BeanPropertyWriter base) {
            super(base);
            this.nameField = new SerializedString(BaseEnumContractSupport.nameFieldOf(base.getName()));
        }

        @Override
        public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov)
                throws Exception {
            super.serializeAsField(bean, gen, prov);
            Object value = get(bean);
            if (value == null) {
                // 镜像原字段的 null 抑制行为
                if (!_suppressNulls) {
                    gen.writeFieldName(nameField);
                    prov.defaultSerializeNull(gen);
                }
                return;
            }
            gen.writeFieldName(nameField);
            gen.writeString(((BaseEnum<?>) value).getName());
        }
    }
}
