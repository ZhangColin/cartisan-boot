package com.cartisan.data.jpa.config;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.WallFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DruidAutoConfiguration 单元测试。
 */
@DisplayName("DruidAutoConfiguration 测试")
class DruidAutoConfigurationTest {

    @Nested
    @SpringBootTest(classes = DruidTestApplication.class)
    @DisplayName("未配置 spring.datasource.type 时")
    class WithoutDruidTypeConfigured {

        @Autowired(required = false)
        private StatFilter statFilter;

        @Autowired(required = false)
        private WallFilter wallFilter;

        @Autowired(required = false)
        private WallConfig wallConfig;

        @Test
        @DisplayName("不应该创建 Druid 相关 Bean")
        void shouldNotCreateDruidBeans() {
            assertThat(statFilter).isNull();
            assertThat(wallFilter).isNull();
            assertThat(wallConfig).isNull();
        }
    }

    @Nested
    @SpringBootTest(classes = DruidTestApplication.class)
    @TestPropertySource(properties = {
        "spring.datasource.type=com.alibaba.druid.pool.DruidDataSource"
    })
    @DisplayName("配置 spring.datasource.type=DruidDataSource 时")
    class WithDruidTypeConfigured {

        @Autowired(required = false)
        private StatFilter statFilter;

        @Autowired(required = false)
        private WallFilter wallFilter;

        @Autowired(required = false)
        private WallConfig wallConfig;

        @Test
        @DisplayName("应该创建 StatFilter Bean")
        void shouldCreateStatFilter() {
            assertThat(statFilter).isNotNull();
        }

        @Test
        @DisplayName("应该创建 WallFilter Bean")
        void shouldCreateWallFilter() {
            assertThat(wallFilter).isNotNull();
        }

        @Test
        @DisplayName("应该创建 WallConfig Bean")
        void shouldCreateWallConfig() {
            assertThat(wallConfig).isNotNull();
        }

        @Test
        @DisplayName("WallConfig 应该允许批量执行")
        void wallConfigShouldAllowMultiStatement() {
            assertThat(wallConfig.isMultiStatementAllow()).isTrue();
        }
    }

    @Nested
    @SpringBootTest(classes = DruidTestApplication.class)
    @TestPropertySource(properties = {
        "spring.datasource.type=com.alibaba.druid.pool.DruidDataSource",
        "spring.datasource.druid.filter.stat.enabled=false"
    })
    @DisplayName("禁用 StatFilter 时")
    class WithStatFilterDisabled {

        @Autowired(required = false)
        private StatFilter statFilter;

        @Autowired(required = false)
        private WallFilter wallFilter;

        @Test
        @DisplayName("不应该创建 StatFilter，但应该创建 WallFilter")
        void shouldNotCreateStatFilter() {
            assertThat(statFilter).isNull();
            assertThat(wallFilter).isNotNull();
        }
    }

    @Nested
    @SpringBootTest(classes = DruidTestApplication.class)
    @TestPropertySource(properties = {
        "spring.datasource.type=com.alibaba.druid.pool.DruidDataSource",
        "spring.datasource.druid.filter.wall.enabled=false"
    })
    @DisplayName("禁用 WallFilter 时")
    class WithWallFilterDisabled {

        @Autowired(required = false)
        private StatFilter statFilter;

        @Autowired(required = false)
        private WallFilter wallFilter;

        @Autowired(required = false)
        private WallConfig wallConfig;

        @Test
        @DisplayName("不应该创建 WallFilter，但应该创建 StatFilter")
        void shouldNotCreateWallFilter() {
            assertThat(statFilter).isNotNull();
            // WallConfig 是独立 Bean，不受 enabled 控制
            assertThat(wallConfig).isNotNull();
            assertThat(wallFilter).isNull();
        }
    }
}
