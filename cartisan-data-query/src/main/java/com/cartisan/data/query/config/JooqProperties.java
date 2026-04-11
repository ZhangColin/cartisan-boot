package com.cartisan.data.query.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * jOOQ 配置属性。
 *
 * <p>配置前缀：{@code cartisan.data-query.jooq}
 *
 * <h2>可配置项</h2>
 * <ul>
 *   <li>{@code sql-logging} — 是否打印 SQL 执行日志，默认 {@code false}</li>
 *   <li>{@code dialect} — SQL 方言，默认 {@code POSTGRES}，可选值参考 {@link org.jooq.SQLDialect}</li>
 * </ul>
 *
 * @since 0.3.0
 */
@ConfigurationProperties("cartisan.data-query.jooq")
public class JooqProperties {

    /**
     * SQL 方言。
     *
     * <p>默认 {@code POSTGRES}。可选值参考 {@link org.jooq.SQLDialect}。</p>
     */
    private String dialect = "POSTGRES";

    public String getDialect() { return dialect; }
    public void setDialect(String dialect) { this.dialect = dialect; }

    /**
     * 是否启用 SQL 执行日志。
     *
     * <p>默认 {@code false}。设置为 {@code true} 后，
     * jOOQ 执行的 SQL 语句会通过 SLF4J 输出（logger: org.jooq.tools.LoggerListener）。
     *
     * @return 是否启用 SQL 日志
     */
    private boolean sqlLogging = false;

    /**
     * 获取 SQL 日志启用状态。
     *
     * @return 是否启用 SQL 日志
     */
    public boolean isSqlLogging() {
        return sqlLogging;
    }

    /**
     * 设置 SQL 日志启用状态。
     *
     * @param sqlLogging 是否启用 SQL 日志
     */
    public void setSqlLogging(boolean sqlLogging) {
        this.sqlLogging = sqlLogging;
    }
}
