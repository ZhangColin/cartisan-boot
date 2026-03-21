package com.cartisan.security.permission;

import com.cartisan.security.integration.IntegrationTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PermissionScanner 集成测试，验证 Bean 在完整 Web 上下文中正确注册。
 */
@SpringBootTest(classes = IntegrationTestApplication.class)
@WebAppConfiguration
class PermissionScannerIntegrationTest {

    @Autowired(required = false)
    private PermissionScanner permissionScanner;

    @Test
    void given_fullWebContext_when_contextLoads_then_permissionScannerBeanExists() {
        assertThat(permissionScanner).isNotNull();
        assertThat(permissionScanner).isInstanceOf(DefaultPermissionScanner.class);
    }
}
