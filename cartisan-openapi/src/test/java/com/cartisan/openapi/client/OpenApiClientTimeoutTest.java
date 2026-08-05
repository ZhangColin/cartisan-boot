package com.cartisan.openapi.client;

import com.cartisan.openapi.config.CartisanOpenapiProperties;
import com.cartisan.openapi.signature.HmacSha256SignatureCalculator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link OpenApiClient} 超时配置测试——证明 {@code cartisan.openapi.timeout.*} 不仅被读取、还被真正应用到出站请求。
 *
 * <p>历史问题：connect/read 超时硬编码 10s/30s（{@code OpenApiClient} 构造器 / get·sendWithBody），
 * 调用方无法收窄；identity 的 app-registry bootstrap 在 SSO 登录热路径上需 3s/5s。本测试钉死「可配 + 真生效」。</p>
 */
class OpenApiClientTimeoutTest {

    @Test
    void shouldDefaultTimeoutsTo10And30Seconds() {
        CartisanOpenapiProperties.Timeout timeout = new CartisanOpenapiProperties().getTimeout();

        assertThat(timeout.getConnectSeconds()).isEqualTo(10L);
        assertThat(timeout.getReadSeconds()).isEqualTo(30L);
    }

    @Test
    void shouldApplyConfiguredReadTimeout() throws Exception {
        // 慢响应服务器：5s 才回——远超配置的 1s 读超时。
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            try {
                Thread.sleep(5_000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.getResponseBody().close();
        });
        server.start();
        String url = "http://localhost:" + server.getAddress().getPort() + "/";

        CartisanOpenapiProperties props = new CartisanOpenapiProperties();
        props.getSelf().setApiKey("timeout-test-key");
        props.getSelf().setApiSecret("timeout-test-secret");
        props.getTimeout().setReadSeconds(1L);
        OpenApiClient client = new OpenApiClient(props, new HmacSha256SignatureCalculator(), new ObjectMapper());

        long start = System.currentTimeMillis();
        try {
            assertThatThrownBy(() -> client.get(url, new TypeReference<Map<String, Object>>() {}))
                .isInstanceOf(RuntimeException.class);
            long elapsed = System.currentTimeMillis() - start;
            // 1s 读超时生效 → 远不到默认 30s（留 3s 上限容机器抖动）
            assertThat(elapsed)
                .as("配置 readSeconds=1 应让请求在 3s 内超时，而非挂默认 30s")
                .isLessThan(3_000L);
        } finally {
            server.stop(0);
        }
    }
}
