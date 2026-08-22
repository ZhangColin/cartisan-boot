package com.cartisan.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContextImpl;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseEnumNameFieldModelConverter 单元测试。
 *
 * <p>验证 swagger schema 与运行时 JSON 两跳同步：BaseEnum 属性在 schema 中
 * 获得 {@code xxxName} 字符串属性，位置紧跟 code 属性之后。</p>
 */
class BaseEnumNameFieldModelConverterTest {

    private final BaseEnumNameFieldModelConverter converter =
            new BaseEnumNameFieldModelConverter(new ObjectMapper());

    /** 仿内置 resolver 的产出：对象 schema，含 BaseEnum/普通枚举/集合/手写展示名属性。 */
    private static ModelConverter objectSchemaResolver() {
        return (type, context, chain) -> {
            Map<String, Schema> properties = new LinkedHashMap<>();
            properties.put("status", new Schema<>().type("integer").description("1=启用, 0=禁用, 2=待审核"));
            properties.put("category", new Schema<>().type("string"));
            properties.put("history", new Schema<>().type("array"));
            return new Schema<>().type("object").properties(properties);
        };
    }

    @Test
    void shouldAddNameFieldImmediatelyAfterBaseEnumProperty() {
        Schema<?> resolved = converter.resolve(
                new AnnotatedType(OrderResponse.class), null, chainOf(objectSchemaResolver()));

        assertThat(resolved.getProperties()).containsKey("statusName");
        Schema<?> statusName = (Schema<?>) resolved.getProperties().get("statusName");
        assertThat(statusName.getType()).isEqualTo("string");
        assertThat(statusName.getDescription()).isEqualTo("status 的展示名");
        // 位置紧跟 status 之后
        assertThat(new ArrayList<>(resolved.getProperties().keySet()))
                .containsSubsequence("status", "statusName");
    }

    @Test
    void shouldNotAddNameFieldForPlainEnumOrCollectionProperties() {
        Schema<?> resolved = converter.resolve(
                new AnnotatedType(OrderResponse.class), null, chainOf(objectSchemaResolver()));

        assertThat(resolved.getProperties())
                .doesNotContainKeys("categoryName", "historyName");
    }

    @Test
    void shouldKeepHandwrittenNameField_whenAlreadyInSchema() {
        Schema<?> handwritten = new StringSchema().description("手写展示名");
        ModelConverter next = (type, context, chain) -> {
            Map<String, Schema> properties = new LinkedHashMap<>();
            properties.put("status", new Schema<>().type("integer"));
            properties.put("statusName", handwritten);
            return new Schema<>().type("object").properties(properties);
        };

        Schema<?> resolved = converter.resolve(
                new AnnotatedType(StatusOnlyResponse.class), null, chainOf(next));

        assertThat(resolved.getProperties().get("statusName")).isSameAs(handwritten);
    }

    @Test
    void shouldAugmentDefinedModel_whenResolvedAsRef() {
        // springdoc 顶层解析常返回 $ref：完整模型在 context.getDefinedModels() 中
        Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put("status", new Schema<>().type("integer"));
        Schema<?> defined = new Schema<>().type("object").properties(properties);

        ModelConverterContextImpl context = new ModelConverterContextImpl(
                (type, ctx, chain) -> null);
        context.defineModel("StatusOnlyResponse", defined);
        ModelConverter next = (type, ctx, chain) ->
                new Schema<>().$ref("#/components/schemas/StatusOnlyResponse");

        Schema<?> resolved = converter.resolve(
                new AnnotatedType(StatusOnlyResponse.class), context, chainOf(next));

        assertThat(resolved.get$ref()).isEqualTo("#/components/schemas/StatusOnlyResponse");
        assertThat(context.getDefinedModels().get("StatusOnlyResponse").getProperties())
                .containsKey("statusName");
    }

    @Test
    void shouldLeaveSchemaUntouched_whenNoPropertiesAndNoRef() {
        Schema<?> bare = new Schema<>().type("object");

        Schema<?> resolved = converter.resolve(
                new AnnotatedType(StatusOnlyResponse.class), null,
                chainOf((type, context, chain) -> bare));

        assertThat(resolved).isSameAs(bare);
        assertThat(resolved.getProperties()).isNull();
    }

    @Test
    void shouldSkipBaseEnumTypeItself() {
        Schema<?> enumSchema = new Schema<>().type("integer");

        Schema<?> resolved = converter.resolve(
                new AnnotatedType(TestUserStatus.class), null,
                chainOf((type, context, chain) -> enumSchema));

        assertThat(resolved).isSameAs(enumSchema);
        assertThat(resolved.getProperties()).isNull();
    }

    @Test
    void shouldUseNamingStrategyAwareExternalNames() {
        // 与序列化侧约定一致：展示名字段名 = 序列化属性名（命名策略应用后）+ "Name"
        BaseEnumNameFieldModelConverter snakeCaseConverter =
                new BaseEnumNameFieldModelConverter(
                        new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE));
        ModelConverter next = (type, context, chain) -> {
            Map<String, Schema> properties = new LinkedHashMap<>();
            properties.put("user_status", new Schema<>().type("integer"));
            return new Schema<>().type("object").properties(properties);
        };

        Schema<?> resolved = snakeCaseConverter.resolve(
                new AnnotatedType(SnakeCaseResponse.class), null, chainOf(next));

        assertThat(resolved.getProperties()).containsKey("user_statusName");
    }

    @Test
    void shouldSkipGracefully_whenRefTargetNotYetDefined() {
        // 循环引用的中间路径：A 尚未完成定义时，B 对 A 的解析只拿到前向 $ref，
        // definedModels 中还没有 A——静默跳过（不抛异常、不补字段），
        // A 的最外层解析完成定义后会再经过本 converter 补齐
        ModelConverterContextImpl context = new ModelConverterContextImpl(
                (type, ctx, chain) -> null);
        ModelConverter next = (type, ctx, chain) ->
                new Schema<>().$ref("#/components/schemas/NotYetDefined");

        Schema<?> resolved = converter.resolve(
                new AnnotatedType(StatusOnlyResponse.class), context, chainOf(next));

        assertThat(resolved.get$ref()).isEqualTo("#/components/schemas/NotYetDefined");
        assertThat(context.getDefinedModels()).doesNotContainKey("NotYetDefined");
    }

    @Test
    void shouldReturnNull_whenChainExhausted() {
        Schema<?> resolved = converter.resolve(
                new AnnotatedType(StatusOnlyResponse.class), null,
                Collections.emptyIterator());

        assertThat(resolved).isNull();
    }

    // ---------- 支撑 ----------

    private static Iterator<ModelConverter> chainOf(ModelConverter next) {
        return List.of(next).iterator();
    }

    record OrderResponse(TestUserStatus status, PlainCategory category, List<TestUserStatus> history) {
    }

    record StatusOnlyResponse(TestUserStatus status) {
    }

    record SnakeCaseResponse(TestUserStatus userStatus) {
    }

    enum PlainCategory {
        PHYSICAL, DIGITAL
    }
}
