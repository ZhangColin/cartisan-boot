package com.cartisan.web.resubmit;

import com.cartisan.web.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ResubmitAspect 集成测试。
 *
 * <p>验证 @PreventResubmit 注解在真实 Spring 环境中正确工作。</p>
 */
@SpringBootTest(classes = {
        PreventResubmitIntegrationTest.TestConfig.class
})
@AutoConfigureMockMvc
class PreventResubmitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeResubmitLock fakeResubmitLock;

    @BeforeEach
    void setUp() {
        // 每次测试前重置 FakeResubmitLock 状态
        fakeResubmitLock.reset();
    }

    @Test
    void shouldAllowFirstRequest() throws Exception {
        // Given: 默认返回 true
        fakeResubmitLock.setLockResult(true);

        // When: 发起请求
        var result = mockMvc.perform(MockMvcRequestBuilders.post("/test/resubmit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then: 请求成功
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).contains("\"code\":200");
    }

    @Test
    void shouldBlockDuplicateRequest() throws Exception {
        // Given: 第一次成功，第二次失败
        fakeResubmitLock.setLockResult(true, false);

        // When: 第一次发起请求
        mockMvc.perform(MockMvcRequestBuilders.post("/test/resubmit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());

        // And: 第二次请求
        var result = mockMvc.perform(MockMvcRequestBuilders.post("/test/resubmit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then: 第二次请求被拒绝
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getContentAsString()).contains("请勿重复提交");
    }

    @Test
    void shouldGenerateCorrectKey() throws Exception {
        // Given: 默认返回 true
        fakeResubmitLock.setLockResult(true);

        // When: 发起请求
        mockMvc.perform(MockMvcRequestBuilders.post("/test/resubmit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());

        // Then: 验证 key 和 delaySeconds
        assertThat(fakeResubmitLock.getLastKey()).startsWith("resubmit:createUser:");
        assertThat(fakeResubmitLock.getLastDelaySeconds()).isEqualTo(10);
    }

    @Test
    void shouldUseCustomDelaySeconds() throws Exception {
        // Given: 默认返回 true
        fakeResubmitLock.setLockResult(true);

        // When: 发起请求
        mockMvc.perform(MockMvcRequestBuilders.post("/test/resubmit-short-delay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());

        // Then: 验证 delaySeconds
        assertThat(fakeResubmitLock.getLastDelaySeconds()).isEqualTo(1);
    }

    @Test
    void shouldUseEmptyPrefixWhenNotSpecified() throws Exception {
        // Given: 默认返回 true
        fakeResubmitLock.setLockResult(true);

        // When: 发起请求
        mockMvc.perform(MockMvcRequestBuilders.post("/test/resubmit-no-prefix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());

        // Then: 验证 key
        assertThat(fakeResubmitLock.getLastKey()).startsWith("resubmit::");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public FakeResubmitLock fakeResubmitLock() {
            return new FakeResubmitLock();
        }

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public ResubmitAspect resubmitAspect(FakeResubmitLock fakeResubmitLock, ObjectMapper objectMapper) {
            return new ResubmitAspect(fakeResubmitLock, objectMapper);
        }
    }

    /**
     * Fake 实现用于测试，可以控制返回值和捕获调用参数。
     */
    static class FakeResubmitLock extends ResubmitLock {
        private boolean[] lockResults = {true};
        private int lockCallIndex = 0;
        private String lastKey;
        private int lastDelaySeconds;

        public FakeResubmitLock() {
            super(null); // 不需要真实的 RedisTemplate
        }

        public void reset() {
            this.lockResults = new boolean[]{true};
            this.lockCallIndex = 0;
            this.lastKey = null;
            this.lastDelaySeconds = 0;
        }

        public void setLockResult(boolean... results) {
            this.lockResults = results;
            this.lockCallIndex = 0;
        }

        public String getLastKey() {
            return lastKey;
        }

        public int getLastDelaySeconds() {
            return lastDelaySeconds;
        }

        @Override
        public boolean lock(String key, int delaySeconds) {
            this.lastKey = key;
            this.lastDelaySeconds = delaySeconds;

            if (lockCallIndex < lockResults.length) {
                return lockResults[lockCallIndex++];
            }
            return lockResults.length > 0 ? lockResults[lockResults.length - 1] : true;
        }

        @Override
        public String generateKey(String prefix, String identity, String argsHash) {
            return "resubmit:" + prefix + ":" + identity + ":" + argsHash;
        }
    }
}
