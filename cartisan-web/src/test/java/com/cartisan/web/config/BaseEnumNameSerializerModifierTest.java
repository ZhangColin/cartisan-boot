package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseEnumNameSerializerModifier 单元测试。
 *
 * <p>验证为 BaseEnum 属性自动追加 {@code xxxName} 虚拟属性的序列化行为：
 * 追加规则、null 值镜像、手写字段优先、反序列化兼容。</p>
 */
class BaseEnumNameSerializerModifierTest {

    private ObjectMapper mapper;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer((Class) BaseEnum.class, new BaseEnumSerializer());
        module.addDeserializer((Class) TestUserStatus.class, new BaseEnumDeserializer(TestUserStatus.class));
        module.setSerializerModifier(new BaseEnumNameSerializerModifier());
        mapper.registerModule(module);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Test
    void shouldAppendNameFieldAfterCodeField_whenSerializingBaseEnumProperty() throws Exception {
        String json = mapper.writeValueAsString(new UserResponse(TestUserStatus.ACTIVE, "标签"));

        JsonNode node = mapper.readTree(json);
        assertThat(node.get("status").asInt()).isEqualTo(1);
        assertThat(node.get("statusName").asText()).isEqualTo("启用");
        assertThat(json.indexOf("\"status\"")).isLessThan(json.indexOf("\"statusName\""));
    }

    @Test
    void shouldNotAppendNameField_forNonBaseEnumProperty() throws Exception {
        String json = mapper.writeValueAsString(new UserResponse(TestUserStatus.ACTIVE, "标签"));

        JsonNode node = mapper.readTree(json);
        assertThat(node.has("labelName")).isFalse();
    }

    @Test
    void shouldWriteNullName_whenEnumValueNull() throws Exception {
        String json = mapper.writeValueAsString(new UserResponse(null, "标签"));

        JsonNode node = mapper.readTree(json);
        assertThat(node.has("status")).isTrue();
        assertThat(node.get("status").isNull()).isTrue();
        assertThat(node.has("statusName")).isTrue();
        assertThat(node.get("statusName").isNull()).isTrue();
    }

    @Test
    void shouldSuppressNameField_whenNullSuppressedByInclusion() throws Exception {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        String json = mapper.writeValueAsString(new UserResponse(null, "标签"));

        JsonNode node = mapper.readTree(json);
        assertThat(node.has("status")).isFalse();
        assertThat(node.has("statusName")).isFalse();
    }

    @Test
    void shouldKeepHandwrittenNameField_whenAlreadyDeclared() throws Exception {
        String json = mapper.writeValueAsString(
                new HandwrittenResponse(TestUserStatus.ACTIVE, "自定义展示名"));

        JsonNode node = mapper.readTree(json);
        assertThat(node.get("statusName").asText()).isEqualTo("自定义展示名");
        // 不得出现两个 statusName 字段
        int first = json.indexOf("\"statusName\"");
        assertThat(json.indexOf("\"statusName\"", first + 1)).isEqualTo(-1);
    }

    @Test
    void shouldNotAppendNameField_forEnumListProperty() throws Exception {
        String json = mapper.writeValueAsString(new ListResponse(java.util.List.of(TestUserStatus.ACTIVE)));

        JsonNode node = mapper.readTree(json);
        assertThat(node.get("statuses").get(0).asInt()).isEqualTo(1);
        assertThat(node.has("statusesName")).isFalse();
    }

    @Test
    void shouldDeserializeIgnoringNameField_whenRoundTripping() throws Exception {
        String json = "{\"status\":2,\"statusName\":\"待审核\",\"label\":\"x\"}";

        UserResponse response = mapper.readValue(json, UserResponse.class);

        assertThat(response.status()).isEqualTo(TestUserStatus.PENDING);
    }

    @Test
    void shouldUseNamingStrategyAwareFieldName_whenSnakeCaseConfigured() throws Exception {
        // 与 schema 侧约定一致：展示名字段名 = 序列化属性名（命名策略应用后）+ "Name"
        mapper.setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);

        String json = mapper.writeValueAsString(new SnakeCaseResponse(TestUserStatus.ACTIVE));

        JsonNode node = mapper.readTree(json);
        assertThat(node.get("user_status").asInt()).isEqualTo(1);
        assertThat(node.get("user_statusName").asText()).isEqualTo("启用");
    }

    record UserResponse(TestUserStatus status, String label) {
    }

    record SnakeCaseResponse(TestUserStatus userStatus) {
    }

    record HandwrittenResponse(TestUserStatus status, String statusName) {
    }

    record ListResponse(java.util.List<TestUserStatus> statuses) {
    }
}
