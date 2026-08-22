package com.cartisan.test.contract;

import com.cartisan.core.domain.BaseEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * swagger 枚举契约测试基类。
 *
 * <p>运行时 Jackson 已把 BaseEnum 双向序列化为 Integer code，swagger 是前后端契约，
 * {@code /v3/api-docs} 中的 BaseEnum 必须同样渲染为 {@code type=integer} + code→名称对照
 * （cartisan-web 在 classpath 有 springdoc 时自动注册 {@code BaseEnumModelConverter} 完成）。
 * 本基类起全上下文拉取 api-docs，做两个方向的断言：</p>
 * <ul>
 *   <li>反向：任何 BaseEnum 不得渲染为 {@code type=string} + name 枚举</li>
 *   <li>正向：任何携带 code 表描述的节点必须 {@code type=integer}</li>
 * </ul>
 *
 * <p>BaseEnum 清单经类路径扫描收集，新增枚举自动纳入，不随业务演进漂移。
 * 对代表性字段可用 {@link #enumFieldType(JsonNode, String, String)} 抽查，
 * 防「文档中无枚举出现」的空转通过。</p>
 *
 * <h2>使用方式</h2>
 * <pre>{@code
 * class SpringDocEnumContractTest extends SpringDocEnumContractTestBase {
 *     @Override
 *     protected String basePackage() {
 *         return "com.example.app";
 *     }
 *
 *     // 有分组时覆写，逐分组校验：
 *     @Override
 *     protected List<String> apiDocsPaths() {
 *         return List.of("/v3/api-docs/project", "/v3/api-docs/task");
 *     }
 * }
 * }</pre>
 *
 * <p>前提：服务同时引入 cartisan-web 与 springdoc。基类的 {@code @SpringBootTest}
 * 与 {@code @AutoConfigureMockMvc} 均可继承；上下文有特殊需要时在子类重新标注覆盖。</p>
 *
 * @since 0.2.0
 * @see BaseEnum
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class SpringDocEnumContractTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 扫描 BaseEnum 实现的根包（必填），通常为业务根包。
     */
    protected abstract String basePackage();

    /**
     * 要校验的 api-docs 端点。
     *
     * <p>默认全量文档 {@code /v3/api-docs}；配置了 GroupedOpenApi 分组时覆写为
     * 各分组端点（{@code /v3/api-docs/{group}}），逐分组校验。</p>
     */
    protected List<String> apiDocsPaths() {
        return List.of("/v3/api-docs");
    }

    /**
     * 契约主体：全端点扫描，双向断言 BaseEnum 的 schema 渲染。
     */
    @Test
    void should_render_base_enum_as_integer_code_in_api_docs() throws Exception {
        Set<Class<?>> enumClasses = baseEnumClasses();
        assertThat(enumClasses)
                .as("basePackage=%s 下应扫描到至少一个 BaseEnum 实现（扫描失效本测试即空转）", basePackage())
                .isNotEmpty();

        Set<Set<String>> baseEnumNameSets = toNameSets(enumClasses);
        Set<String> codeTables = toCodeTables(enumClasses);

        for (String path : apiDocsPaths()) {
            JsonNode doc = fetchApiDocs(path);

            List<String> stringEnumViolations = new ArrayList<>();
            collectStringEnumViolations(doc, baseEnumNameSets, stringEnumViolations);
            assertThat(stringEnumViolations)
                    .as("端点 %s 中 BaseEnum 不得渲染为 string+name 枚举（运行时 JSON 是 Integer code）", path)
                    .isEmpty();

            List<String> nonInteger = new ArrayList<>();
            collectCodeTableNodesNotInteger(doc, codeTables, nonInteger);
            assertThat(nonInteger)
                    .as("端点 %s 中携带 code 表的 BaseEnum schema 必须 type=integer", path)
                    .isEmpty();
        }
    }

    /**
     * 抽查指定字段的 schema type（内联 type 或 $ref 均可辨），供子类防「无枚举出现」的空转通过。
     *
     * <pre>{@code
     * assertThat(enumFieldType(doc, "ProjectResponse", "status")).isEqualTo("integer");
     * }</pre>
     *
     * @param doc    api-docs 文档
     * @param schema components.schemas 下的 schema 名
     * @param field  属性名
     * @return type 文本，schema/属性缺失时断言失败（视为契约漂移）
     */
    protected static String enumFieldType(JsonNode doc, String schema, String field) {
        JsonNode property = doc.path("components").path("schemas").path(schema)
                .path("properties").path(field);
        assertThat(property.isMissingNode())
                .as("api-docs 应含 %s.%s（缺失视为契约漂移）", schema, field)
                .isFalse();
        return property.path("type").asText(null);
    }

    /**
     * 拉取并解析一个 api-docs 端点。
     */
    protected JsonNode fetchApiDocs(String path) throws Exception {
        MockHttpServletRequestBuilder request = get(path);
        String body = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    // ---------- 扫描与断言支撑 ----------

    /**
     * 类路径扫描 basePackage 下全部 BaseEnum 枚举类（新增枚举自动纳入）。
     */
    private Set<Class<?>> baseEnumClasses() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseEnum.class));
        return scanner.findCandidateComponents(basePackage()).stream()
                .map(candidate -> {
                    try {
                        return Class.forName(candidate.getBeanClassName(), false,
                                getClass().getClassLoader());
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .filter(Class::isEnum)
                .collect(Collectors.toSet());
    }

    /**
     * 反向断言的比对物：各 BaseEnum 的 name 集合。
     */
    private static Set<Set<String>> toNameSets(Set<Class<?>> enumClasses) {
        return enumClasses.stream()
                .map(clazz -> Arrays.stream(clazz.getEnumConstants())
                        .map(value -> ((Enum<?>) value).name())
                        .collect(Collectors.toSet()))
                .collect(Collectors.toSet());
    }

    /**
     * 正向断言的比对物：converter 生成的 code 表串（"1=启用, 0=禁用" 形）。
     * 格式复用 {@link BaseEnum#codeTableOf(Class)}，与 schema description 单点对齐。
     */
    private static Set<String> toCodeTables(Set<Class<?>> enumClasses) {
        return enumClasses.stream()
                .map(BaseEnum::codeTableOf)
                .collect(Collectors.toSet());
    }

    /**
     * 深度遍历（object 节点逐个回调，携带 JSON 路径便于定位违例）：任何 string 型
     * enum 值集与某 BaseEnum 的 name 集相同即违例。
     */
    private static void collectStringEnumViolations(JsonNode doc, Set<Set<String>> baseEnumNameSets,
                                                    List<String> violations) {
        walkObjectNodes(doc, "$", (node, path) -> {
            if ("string".equals(node.path("type").asText()) && node.path("enum").isArray()) {
                Set<String> names = new HashSet<>();
                node.path("enum").forEach(value -> names.add(value.asText()));
                if (baseEnumNameSets.contains(names)) {
                    violations.add(path);
                }
            }
        });
    }

    /**
     * 正向半边：携带 code 表描述的节点（即 BaseEnum 渲染处）必须 type=integer。
     * 用「包含」匹配，兼容 converter 在已有 @Schema description 后追加对照表的形态。
     */
    private static void collectCodeTableNodesNotInteger(JsonNode doc, Set<String> codeTables,
                                                        List<String> violations) {
        walkObjectNodes(doc, "$", (node, path) -> {
            JsonNode description = node.path("description");
            if (description.isMissingNode()) {
                return;
            }
            String text = description.asText();
            if (codeTables.stream().anyMatch(text::contains)
                    && !"integer".equals(node.path("type").asText())) {
                violations.add(path);
            }
        });
    }

    /**
     * 通用遍历：深度走 JSON 树，每个 object 节点回调 visitor（携带从根起的路径）。
     */
    private static void walkObjectNodes(JsonNode node, String path,
                                        BiConsumer<JsonNode, String> visitor) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            visitor.accept(node, path);
            node.fields().forEachRemaining(entry ->
                    walkObjectNodes(entry.getValue(), path + "." + entry.getKey(), visitor));
            return;
        }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                walkObjectNodes(node.get(i), path + "[" + i + "]", visitor);
            }
        }
    }
}
