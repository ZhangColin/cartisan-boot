package com.cartisan.web.context;

import com.cartisan.core.context.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
class RequestContextFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldInitializeContext_whenRequestWithoutRequestId() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/exception"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        assertThat(RequestContext.getRequestId()).isNull();
    }

    @Test
    void shouldUseHeader_whenRequestWithRequestId() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/exception")
                        .header("X-Request-Id", "integration-test-123"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        assertThat(RequestContext.getRequestId()).isNull();
    }

    @Test
    void shouldCleanContextAfterRequest() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/exception"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        assertThat(RequestContext.getRequestId()).isNull();
        assertThat(RequestContext.getClientIp()).isNull();
    }
}
