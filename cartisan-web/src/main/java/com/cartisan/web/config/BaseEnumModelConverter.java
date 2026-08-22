package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.beans.factory.DisposableBean;

import java.util.Iterator;

/**
 * BaseEnum 的 springdoc 全局 ModelConverter：swagger schema 对齐运行时 JSON 契约。
 *
 * <p>运行时 Jackson 已通过 {@code serializerByType} 把 BaseEnum 双向序列化为 Integer code
 * （见 {@link JacksonConfiguration}），但 springdoc 不感知该注册，默认把 BaseEnum 渲染成
 * {@code type=string} + name 枚举（如 {@code "ACTIVE|DISABLED"}）——前端按 swagger 生成的
 * 类型与运行时 JSON 冲突。</p>
 *
 * <p>本 converter 把 BaseEnum schema 改写为自描述契约：</p>
 * <ul>
 *   <li>{@code type=integer}（与运行时 Integer code 一致）</li>
 *   <li>清除默认的 name 枚举值</li>
 *   <li>description 附完整 code→名称对照（如 {@code 1=启用, 0=禁用}），
 *       已有 {@code @Schema(description=...)} 保留在前，对照表追加其后</li>
 * </ul>
 *
 * <p>实现为先交给链条后续 converter 解析（保留 {@code @Schema} 注解的其他装饰），
 * 再对 BaseEnum 结果原地改写；非 BaseEnum 类型原样透传。泛型包装（如
 * {@code ApiResponse<T>}）的 resolved type 裸类是包装类本身，不改写——内嵌的
 * BaseEnum 属性解析时会各自经过本 converter，同样生效。</p>
 *
 * <p>由 {@code CartisanWebAutoConfiguration} 在 classpath 存在 springdoc 时注册为
 * Bean，springdoc 的 ModelConverterRegistrar 收集容器内全部 {@link ModelConverter}
 * 自动接入全局解析链。禁止逐字段 {@code @Schema} 硬编码 code 对照——会随枚举演进漂移。</p>
 *
 * @since 0.2.0
 * @see BaseEnum
 */
public class BaseEnumModelConverter implements ModelConverter, DisposableBean {

    /**
     * 上下文关闭时从全局单例移除自身。
     *
     * <p>springdoc 把 ModelConverter Bean 注册进 JVM 级静态单例
     * {@link ModelConverters} 且不做清理；不注销时同 JVM 内每次上下文重建
     * （devtools 重启、多 {@code @SpringBootTest}）都会累积重复实例。</p>
     */
    @Override
    public void destroy() {
        BaseEnumContractSupport.deregisterFromGlobalSingletons(this);
    }

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context,
                             Iterator<ModelConverter> chain) {
        Class<?> rawClass = BaseEnumContractSupport.rawClassOf(type);
        if (rawClass == null || !BaseEnum.class.isAssignableFrom(rawClass)) {
            return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        }

        Schema<?> schema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        if (schema == null) {
            schema = new Schema<>();
        }
        schema.setType("integer");
        schema.setEnum(null);
        schema.setDescription(withCodeTable(schema.getDescription(), rawClass));
        return schema;
    }

    /**
     * 把对照表并入已有描述：空描述直接用对照表；已含对照表（重复解析）不重复追加。
     * 对照表格式单点定义在 {@link BaseEnum#codeTableOf(Class)}。
     */
    private static String withCodeTable(String description, Class<?> enumClass) {
        String codeTable = BaseEnum.codeTableOf(enumClass);
        if (description == null || description.isBlank()) {
            return codeTable;
        }
        return description.contains(codeTable) ? description
                : description + "（" + codeTable + "）";
    }
}
