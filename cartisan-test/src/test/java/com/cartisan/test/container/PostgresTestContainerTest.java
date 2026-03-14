package com.cartisan.test.container;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

@DisplayName("PostgresTestContainer 单元测试")
class PostgresTestContainerTest {

    @Test
    @DisplayName("postgres() 方法应返回配置好的 PostgreSQLContainer")
    void given_whenPostgres_thenReturnsConfiguredContainer() {
        // When
        PostgreSQLContainer<?> container = PostgresTestContainer.postgres();

        // Then
        assertThat(container).isNotNull();
        assertThat(container.getDatabaseName()).isEqualTo("testdb");
        assertThat(container.getUsername()).isEqualTo("test");
        assertThat(container.getPassword()).isEqualTo("test");
    }

    @Test
    @DisplayName("应有 @TestConfiguration 注解")
    void given_whenCheckAnnotation_thenHasTestConfiguration() {
        // Then
        assertThat(PostgresTestContainer.class.isAnnotationPresent(
            org.springframework.boot.test.context.TestConfiguration.class
        )).isTrue();
    }
}
