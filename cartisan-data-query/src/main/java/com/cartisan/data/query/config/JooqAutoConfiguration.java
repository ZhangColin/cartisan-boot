package com.cartisan.data.query.config;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

import static org.jooq.impl.DSL.using;

/**
 * cartisan-data-query 模块的 jOOQ 自动配置。
 *
 * <p>配置内容：
 * <ul>
 *   <li>{@link DSLContext} — jOOQ 核心 API 入口，基于项目的 {@link DataSource} 创建</li>
   *   <li>SQL 方言 — 通过 {@link JooqProperties#getDialect()} 配置，默认 PostgreSQL</li>
 *   <li>SQL 日志 — 根据 {@link JooqProperties#isSqlLogging()} 决定是否启用</li>
 * </ul>
 *
 * <h2>装配条件</h2>
 * <ul>
 *   <li>存在 {@link DataSource} Bean 时生效</li>
 *   <li>不存在用户自定义的 {@link DSLContext} Bean 时生效</li>
 * </ul>
 *
 * <h2>用户覆盖</h2>
 * <p>若需自定义 {@code DSLContext}（如多数据源、特殊配置），
 * 用户可注册自己的 Bean，自动配置会自动退让。</p>
 *
 * @since 0.3.0
 */
@AutoConfiguration
@ConditionalOnBean(DataSource.class)
@ConditionalOnMissingBean(DSLContext.class)
@EnableConfigurationProperties(JooqProperties.class)
public class JooqAutoConfiguration {

    /**
     * 创建 jOOQ {@link DSLContext} Bean。
     *
     * <p>配置要点：
     * <ul>
     *   <li>使用项目已有的 {@link DataSource}</li>
     *   <li>方言通过配置属性指定，默认 PostgreSQL</li>
     *   <li>根据 {@code cartisan.data-query.jooq.sql-logging} 决定是否打印 SQL</li>
     * </ul>
     *
     * @param dataSource 数据源，由 Spring 注入
     * @param properties jOOQ 配置属性
     * @return DSLContext 实例
     */
    @Bean
    public DSLContext dslContext(DataSource dataSource, JooqProperties properties) {
        Settings settings = new Settings()
            .withExecuteLogging(properties.isSqlLogging());
        return using(dataSource, SQLDialect.valueOf(properties.getDialect().toUpperCase()), settings);
    }
}
