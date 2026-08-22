package com.cartisan.test.mvc;

import com.cartisan.test.context.WithRequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link CartisanMvcTest} 单元测试。
 *
 * <p>验证切片自动带上 cartisan-web 的枚举绑定、Jackson 枚举序列化与全局异常处理，
 * 全程零手工 {@code @Import} cartisan-web 配置。</p>
 */
@CartisanMvcTest(controllers = MvcOrderController.class)
class CartisanMvcTestTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void shouldBindEnumQueryParam_whenCodeGiven() throws Exception {
        mvc.perform(get("/test/orders/query").param("status", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value("ORD-001"));
    }

    @Test
    void shouldBindEnumPathVariable_whenCodeGiven() throws Exception {
        mvc.perform(get("/test/orders/2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value("ORD-002"));
    }

    @Test
    void shouldSerializeBaseEnumFieldAsIntegerCode() throws Exception {
        mvc.perform(get("/test/orders/query").param("status", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(1));
    }

    @Test
    void shouldReturn400Envelope_whenQueryParamCodeInvalid() throws Exception {
        mvc.perform(get("/test/orders/query").param("status", "999"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value(allOf(
                containsString("status"),
                containsString("999"),
                containsString("1=已支付"))));
    }

    @Test
    void shouldReturn400Envelope_whenQueryParamCodeNotNumeric() throws Exception {
        mvc.perform(get("/test/orders/query").param("status", "abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value(containsString("abc")));
    }

    @Test
    void shouldDeserializeEnumFromRequestBodyCode() throws Exception {
        mvc.perform(post("/test/orders")
                .contentType("application/json")
                .content("{\"status\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value("ORD-003"))
            .andExpect(jsonPath("$.status").value(2));
    }

    @Test
    void shouldReturn400Envelope_whenRequestBodyCodeInvalid() throws Exception {
        mvc.perform(post("/test/orders")
                .contentType("application/json")
                .content("{\"status\":999}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value(containsString("999")));
    }

    @Test
    void shouldNotMapController_whenNotSpecifiedInSlice() throws Exception {
        mvc.perform(get("/test/other"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithRequestContext(userId = 42)
    void shouldExposeRequestContext_whenCombinedWithWithRequestContext() throws Exception {
        mvc.perform(get("/test/orders/context"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("42"));
    }
}
