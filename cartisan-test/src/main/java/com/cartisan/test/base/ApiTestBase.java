package com.cartisan.test.base;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * API 测试基类。
 *
 * <p>提供 MockMvc 能力，用于测试 Controller 层。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * class OrderControllerTest extends ApiTestBase {
 *
 *     @Autowired
 *     private ObjectMapper objectMapper;
 *
 *     @Test
 *     void shouldCreateOrder() throws Exception {
 *         String orderJson = objectMapper.writeValueAsString(new CreateOrderRequest("O001"));
 *
 *         mvc.perform(post("/api/v1/orders")
 *                 .contentType(MediaType.APPLICATION_JSON)
 *                 .content(orderJson))
 *             .andExpect(status().isOk())
 *             .andExpect(jsonPath("$.data.id").value("O001"));
 *     }
 * }
 * }</pre>
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>不封装 MockMvc API — 保持薄基类</li>
 *   <li>业务项目直接使用 MockMvc fluent API</li>
 *   <li>获得完整的灵活性（查询参数、Header、multipart 等）</li>
 * </ul>
 *
 * @since 0.1.0
 */
@AutoConfigureMockMvc
public abstract class ApiTestBase {

    @Autowired
    protected MockMvc mvc;
}
