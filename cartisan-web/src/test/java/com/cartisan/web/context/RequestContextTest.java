package com.cartisan.web.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * RequestContext 单元测试。
 *
 * <p>测试命名遵循 given_*_when_*_then_* 格式</p>
 */
@Execution(ExecutionMode.SAME_THREAD) // 确保测试顺序执行，避免 ThreadLocal 干扰
class RequestContextTest {

    @AfterEach
    void tearDown() {
        // 每个测试后清理，避免互相干扰
        RequestContext.clear();
    }

    @Test
    void given_noContext_when_getRequestId_then_returnsNull() {
        // Given: 未初始化 RequestContext

        // When: 调用 getRequestId
        String requestId = RequestContext.getRequestId();

        // Then: 返回 null
        assertThat(requestId).isNull();
    }

    @Test
    void given_noContext_when_getClientIp_then_returnsNull() {
        // Given: 未初始化 RequestContext

        // When: 调用 getClientIp
        String clientIp = RequestContext.getClientIp();

        // Then: 返回 null
        assertThat(clientIp).isNull();
    }

    @Test
    void given_initContextWithValues_when_getRequestId_then_returnsInitializedValue() {
        // Given: 初始化 RequestContext
        RequestContext.init("test-request-id", "192.168.1.1");

        // When: 调用 getRequestId
        String requestId = RequestContext.getRequestId();

        // Then: 返回初始化的值
        assertThat(requestId).isEqualTo("test-request-id");
    }

    @Test
    void given_initContextWithValues_when_getClientIp_then_returnsInitializedValue() {
        // Given: 初始化 RequestContext
        RequestContext.init("test-request-id", "192.168.1.1");

        // When: 调用 getClientIp
        String clientIp = RequestContext.getClientIp();

        // Then: 返回初始化的值
        assertThat(clientIp).isEqualTo("192.168.1.1");
    }

    @Test
    void given_initContextWithNullValues_when_getRequestId_then_returnsNull() {
        // Given: 初始化 RequestContext，值为 null
        RequestContext.init(null, null);

        // When: 调用 getRequestId
        String requestId = RequestContext.getRequestId();

        // Then: 返回 null
        assertThat(requestId).isNull();
    }

    @Test
    void given_initContextWithNullValues_when_getClientIp_then_returnsNull() {
        // Given: 初始化 RequestContext，值为 null
        RequestContext.init(null, null);

        // When: 调用 getClientIp
        String clientIp = RequestContext.getClientIp();

        // Then: 返回 null
        assertThat(clientIp).isNull();
    }

    @Test
    void given_initContext_when_clear_then_getRequestIdReturnsNull() {
        // Given: 初始化 RequestContext
        RequestContext.init("test-request-id", "192.168.1.1");

        // When: 清理上下文
        RequestContext.clear();

        // Then: getRequestId 返回 null
        assertThat(RequestContext.getRequestId()).isNull();
    }

    @Test
    void given_initContext_when_clear_then_getClientIpReturnsNull() {
        // Given: 初始化 RequestContext
        RequestContext.init("test-request-id", "192.168.1.1");

        // When: 清理上下文
        RequestContext.clear();

        // Then: getClientIp 返回 null
        assertThat(RequestContext.getClientIp()).isNull();
    }

    @Test
    void given_initContextTwice_when_getRequestId_then_returnsLatestValue() {
        // Given: 第一次初始化
        RequestContext.init("first-request-id", "192.168.1.1");

        // When: 第二次初始化（覆盖）
        RequestContext.init("second-request-id", "192.168.1.2");

        // Then: 返回第二次的值
        assertThat(RequestContext.getRequestId()).isEqualTo("second-request-id");
        assertThat(RequestContext.getClientIp()).isEqualTo("192.168.1.2");
    }

    @Test
    void given_concurrentAccess_when_multipleThreads_then_noInterference() throws InterruptedException {
        // Given: 多个线程
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        String[] results = new String[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            final String requestId = "request-" + i;

            threads[i] = new Thread(() -> {
                try {
                    // When: 每个线程初始化自己的 RequestContext
                    RequestContext.init(requestId, "127.0.0.1");
                    // 短暂等待，增加并发冲突概率
                    Thread.sleep(10);
                    // Then: 读取自己的值
                    results[index] = RequestContext.getRequestId();
                } catch (Exception e) {
                    exceptions[index] = e;
                } finally {
                    try {
                        RequestContext.clear();
                    } catch (UnsupportedOperationException ignored) {
                        // Step 2 阶段 clear() 未实现，忽略
                    }
                }
            });
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // Then: 每个线程都获取到自己的 requestId，没有串扰
        for (int i = 0; i < threadCount; i++) {
            assertThat(exceptions[i]).as("Thread %d threw exception", i).isNull();
            assertThat(results[i]).as("Thread %d result", i)
                    .isEqualTo("request-" + i);
        }
    }
}
