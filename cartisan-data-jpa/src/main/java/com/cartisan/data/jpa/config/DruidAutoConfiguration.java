package com.cartisan.data.jpa.config;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.WallFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Druid 数据源自动配置。
 *
 * <p>配置内容：
 * <ul>
 *   <li>StatFilter — SQL 监控和慢 SQL 记录</li>
 *   <li>WallFilter — SQL 防火墙，防止 SQL 注入</li>
 * </ul>
 *
 * <h3>装配条件</h3>
 * <ul>
 *   <li>classpath 中存在 {@code com.alibaba.druid.pool.DruidDataSource}</li>
 *   <li>配置了 {@code spring.datasource.type=com.alibaba.druid.pool.DruidDataSource}</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <p>业务项目在 {@code application.yml} 中配置：</p>
 * <pre>{@code
 * spring:
 *   datasource:
 *     type: com.alibaba.druid.pool.DruidDataSource
 *     druid:
 *       stat-view-servlet:
 *         enabled: true
 *         login-username: admin
 *         login-password: admin
 * }</pre>
 *
 * <p>监控页面访问地址：{@code http://localhost:8080/druid/index.html}</p>
 */
@AutoConfiguration
@ConditionalOnClass(name = "com.alibaba.druid.pool.DruidDataSource")
@ConditionalOnProperty(
    name = "spring.datasource.type",
    havingValue = "com.alibaba.druid.pool.DruidDataSource"
)
public class DruidAutoConfiguration {

    /**
     * 配置 StatFilter（SQL 监控过滤器）。
     *
     * <p>默认启用，可通过 {@code spring.datasource.druid.filter.stat.enabled=false} 禁用。</p>
     *
     * <p>业务可通过配置覆盖慢 SQL 阈值等参数：</p>
     * <pre>{@code
     * spring:
     *   datasource:
     *     druid:
     *       filter:
     *         stat:
     *           log-slow-sql: true
     *           slow-sql-millis: 1000
     * }</pre>
     *
     * @return StatFilter Bean
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "spring.datasource.druid.filter.stat",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public StatFilter statFilter() {
        return new StatFilter();
    }

    /**
     * 配置 WallConfig（防火墙配置）。
     *
     * <p>默认允许批量执行，可通过配置覆盖。</p>
     *
     * @return WallConfig Bean
     */
    @Bean
    public WallConfig wallConfig() {
        WallConfig config = new WallConfig();
        // 允许批量执行，兼容 JPA 批量操作
        config.setMultiStatementAllow(true);
        return config;
    }

    /**
     * 配置 WallFilter（SQL 防火墙过滤器）。
     *
     * <p>默认启用，可通过 {@code spring.datasource.druid.filter.wall.enabled=false} 禁用。</p>
     *
     * @param wallConfig WallConfig Bean
     * @return WallFilter Bean
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "spring.datasource.druid.filter.wall",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public WallFilter wallFilter(WallConfig wallConfig) {
        WallFilter filter = new WallFilter();
        filter.setConfig(wallConfig);
        return filter;
    }
}
