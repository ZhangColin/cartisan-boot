package com.cartisan.web.request;

import com.cartisan.web.TestApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 分页参数 MVC 绑定端到端集成测试（#29）。
 *
 * <p>覆盖 wire 扁平绑定、参数缺省、clamp、非数值 → 400 field-error 信封、
 * 排序白名单 400、Ordering 独立绑定、超尾页回显。</p>
 */
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@DisplayName("分页参数 MVC 绑定集成测试")
class PaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("扁平绑定与 clamp")
    class FlatBinding {

        @Test
        @DisplayName("page/size/sort 顶级参数绑定成功且 offset/limit 直出")
        void shouldBindFlatParameters() throws Exception {
            mockMvc.perform(get("/pagination-test/list")
                            .queryParam("page", "2")
                            .queryParam("size", "50")
                            .queryParam("sort", "createdAt,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.size").value(50))
                    .andExpect(jsonPath("$.sort[0]").value("createdAt"))
                    .andExpect(jsonPath("$.sort[1]").value("desc"))
                    .andExpect(jsonPath("$.offset").value(50))
                    .andExpect(jsonPath("$.limit").value(50));
        }

        @Test
        @DisplayName("参数缺省绑定 page=1、size=20、空排序")
        void shouldApplyDefaults_whenParamsAbsent() throws Exception {
            mockMvc.perform(get("/pagination-test/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.sort").isEmpty())
                    .andExpect(jsonPath("$.offset").value(0))
                    .andExpect(jsonPath("$.limit").value(20));
        }

        @Test
        @DisplayName("越界数值静默贴边：page=0→1、size=1000→100")
        void shouldClampOutOfRangeValues() throws Exception {
            mockMvc.perform(get("/pagination-test/list")
                            .queryParam("page", "0")
                            .queryParam("size", "1000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.size").value(100))
                    .andExpect(jsonPath("$.limit").value(100));
        }

        @Test
        @DisplayName("sort 多值参数与单值逗号展平为同一 token 流")
        void shouldFlattenMultiValueSort() throws Exception {
            mockMvc.perform(get("/pagination-test/list")
                            .queryParam("sort", "id,asc")
                            .queryParam("sort", "createdAt,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sort", hasSize(4)))
                    .andExpect(jsonPath("$.sort[0]").value("id"))
                    .andExpect(jsonPath("$.sort[1]").value("asc"))
                    .andExpect(jsonPath("$.sort[2]").value("createdAt"))
                    .andExpect(jsonPath("$.sort[3]").value("desc"));
        }
    }

    @Nested
    @DisplayName("错误信封")
    class ErrorEnvelope {

        @Test
        @DisplayName("非数值 page → 400 field-error 信封（BindException 路径）")
        void shouldReturn400FieldError_whenPageNotNumeric() throws Exception {
            mockMvc.perform(get("/pagination-test/list")
                            .queryParam("page", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.errors[0].field").value("page"));
        }

        @Test
        @DisplayName("白名单外排序字段 → 400 通用文案信封")
        void shouldReturn400_whenSortFieldOutsideWhitelist() throws Exception {
            mockMvc.perform(get("/pagination-test/whitelisted")
                            .queryParam("page", "1")
                            .queryParam("size", "20")
                            .queryParam("sort", "password,desc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("Invalid request"));
        }
    }

    @Nested
    @DisplayName("白名单转换端到端")
    class WhitelistConversion {

        @Test
        @DisplayName("白名单内排序经 toPageRequest 转为 0-based 页码")
        void shouldConvertWithWhitelist_whenFieldAllowed() throws Exception {
            mockMvc.perform(get("/pagination-test/whitelisted")
                            .queryParam("page", "3")
                            .queryParam("size", "10")
                            .queryParam("sort", "createdAt,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pageNumber").value(2))
                    .andExpect(jsonPath("$.pageSize").value(10));
        }
    }

    @Nested
    @DisplayName("Ordering 独立绑定")
    class OrderingBinding {

        @Test
        @DisplayName("不分页端点收 Ordering，token 流解析为属性:方向")
        void shouldBindOrderingAndParseSort() throws Exception {
            mockMvc.perform(get("/pagination-test/ordering")
                            .queryParam("sort", "name,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0]").value("name:DESC"));
        }

        @Test
        @DisplayName("Ordering 缺省无排序参数返回空数组")
        void shouldReturnEmptyArray_whenSortAbsent() throws Exception {
            mockMvc.perform(get("/pagination-test/ordering"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("回显")
    class Echo {

        @Test
        @DisplayName("超尾页返回空列表并原样回显请求页码（0-based 99 → 1-based 100）")
        void shouldEchoRequestedPage_whenBeyondLastPage() throws Exception {
            mockMvc.perform(get("/pagination-test/page-response"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isEmpty())
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.page").value(100))
                    .andExpect(jsonPath("$.size").value(20));
        }
    }
}
