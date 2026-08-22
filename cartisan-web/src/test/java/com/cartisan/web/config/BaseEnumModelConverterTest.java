package com.cartisan.web.config;

import com.cartisan.web.response.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseEnumModelConverter 单元测试。
 *
 * <p>验证 swagger schema 对齐运行时 JSON 契约：BaseEnum 双向序列化为 Integer code，
 * schema 必须渲染为 type=integer + code→名称对照，而非默认的 string+name 枚举。</p>
 */
class BaseEnumModelConverterTest {

    private final BaseEnumModelConverter converter = new BaseEnumModelConverter();

    /** 内置 resolver 对枚举的典型产出：type=string + name 枚举（可携带 @Schema description）。 */
    private static ModelConverter builtinLikeResolver() {
        return (type, context, chain) -> {
            Schema<String> schema = new Schema<>().type("string").description("用户状态");
            schema.addEnumItemObject("ACTIVE");
            schema.addEnumItemObject("DISABLED");
            schema.addEnumItemObject("PENDING");
            return schema;
        };
    }

    @Test
    void should_rewrite_base_enum_as_integer_with_code_table() {
        Schema<?> resolved = converter.resolve(
                new AnnotatedType(TestUserStatus.class), null, chainOf(builtinLikeResolver()));

        assertThat(resolved).isNotNull();
        assertThat(resolved.getType()).isEqualTo("integer");
        // name 枚举必须清除，否则前端生成器仍按 name 产出类型
        assertThat(resolved.getEnum()).isNull();
        // 原 @Schema description 保留，code 表追加其上
        assertThat(resolved.getDescription())
                .contains("用户状态")
                .contains("1=启用")
                .contains("0=禁用")
                .contains("2=待审核");
    }

    @Test
    void should_keep_plain_enum_schema_unchanged() {
        // 内置 resolver 对普通枚举的正常产出：string + name 枚举，必须原样透传
        Schema<String> plainEnumSchema = new Schema<>().type("string");
        plainEnumSchema.addEnumItemObject("PHYSICAL");
        plainEnumSchema.addEnumItemObject("DIGITAL");

        Schema<?> resolved = converter.resolve(
                new AnnotatedType(PlainCategory.class), null,
                chainOf((type, context, chain) -> plainEnumSchema));

        assertThat(resolved).isSameAs(plainEnumSchema);
        assertThat(resolved.getType()).isEqualTo("string");
        assertThat(resolved.getEnum()).extracting(Object::toString)
                .containsExactly("PHYSICAL", "DIGITAL");
    }

    @Test
    void should_create_fresh_integer_schema_when_chain_exhausted() {
        Schema<?> resolved = converter.resolve(
                new AnnotatedType(TestUserStatus.class), null, emptyChain());

        assertThat(resolved).isNotNull();
        assertThat(resolved.getType()).isEqualTo("integer");
        assertThat(resolved.getEnum()).isNull();
        assertThat(resolved.getDescription())
                .isEqualTo("1=启用, 0=禁用, 2=待审核");
    }

    @Test
    void should_return_null_for_non_base_enum_when_chain_exhausted() {
        Schema<?> resolved = converter.resolve(
                new AnnotatedType(PlainCategory.class), null, emptyChain());

        assertThat(resolved).isNull();
    }

    @Test
    void should_not_treat_generic_wrapper_as_base_enum() {
        // 泛型包装（如 ApiResponse<TestUserStatus>）的 resolved type 裸类是包装类本身，
        // 不做改写，交给链条正常解析（内嵌属性会各自走一遍 converter）
        Type wrapperType = new TypeReference<ApiResponse<TestUserStatus>>() {}.getType();
        Schema<?> stubSchema = new Schema<>().type("object");
        ModelConverter next = (type, context, chain) -> stubSchema;

        Schema<?> resolved = converter.resolve(
                new AnnotatedType(wrapperType), null, chainOf(next));

        assertThat(resolved).isSameAs(stubSchema);
        assertThat(resolved.getType()).isEqualTo("object");
    }

    @Test
    void should_handle_null_and_unresolvable_type() {
        assertThat(converter.resolve(null, null, emptyChain())).isNull();

        // 无法解析裸类的 Type 交还链条，不抛异常
        ModelConverter next = (type, context, chain) -> null;
        assertThat(converter.resolve(
                new AnnotatedType(new TypeReference<List<TestUserStatus>>() {}.getType()),
                null, chainOf(next))).isNull();
    }

    @Test
    void should_not_duplicate_code_table_on_repeated_resolution() {
        Schema<?> schema = new Schema<>().description("用户状态（1=启用, 0=禁用, 2=待审核）");
        ModelConverter next = (type, context, chain) -> schema;

        Schema<?> resolved = converter.resolve(
                new AnnotatedType(TestUserStatus.class), null, chainOf(next));

        assertThat(resolved.getDescription()).isEqualTo("用户状态（1=启用, 0=禁用, 2=待审核）");
    }

    // ---------- 支撑 ----------

    private static Iterator<ModelConverter> chainOf(ModelConverter next) {
        return List.of(next).iterator();
    }

    private static Iterator<ModelConverter> emptyChain() {
        return Collections.emptyIterator();
    }

    /** 未实现 BaseEnum 的普通枚举（schema 必须不受影响）。 */
    enum PlainCategory {
        PHYSICAL, DIGITAL
    }
}
