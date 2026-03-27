# Druid 数据源集成设计文档

> **版本**: v0.1
> **日期**: 2026-03-27
> **模块**: cartisan-data-jpa

---

## 一、背景

### 1.1 需求来源

用户反馈之前框架使用 Druid 数据源，看重以下功能：
1. **监控页面** - SQL 监控、URI 监控、Session 监控
2. **慢 SQL 记录** - 自动记录超过阈值的 SQL
3. **防火墙** - SQL 注入检测
4. **连接池监控** - 活跃连接、等待连接数等

当前 cartisan-boot 框架使用 Spring Boot 默认的 HikariCP，未集成 Druid。

### 1.2 设计目标

- 在 `cartisan-data-jpa` 模块中集成 Druid
- 使用 Druid 原生配置方式，不引入自定义配置节点
- 业务配置可覆盖框架默认配置
- 保持框架的零配置启动特性

---

## 二、方案设计

### 2.1 架构决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 集成位置 | `cartisan-data-jpa` 模块内 | 与 JPA 写侧绑定，逻辑内聚 |
| 配置方式 | Druid 原生 `spring.datasource.druid.*` | 简洁、用户熟悉、无需学习 |
| 监控路径 | 默认 `/druid/*` | 与现有 RequestLogFilter 排除规则一致 |
| 启用方式 | 业务配置 `spring.datasource.type` | 保持框架默认 HikariCP，业务按需切换 |

### 2.2 模块结构

```
cartisan-data-jpa/
├── src/main/java/com/cartisan/data/jpa/
│   └── config/
│       ├── CartisanDataJpaAutoConfiguration.java  (已存在)
│       └── DruidAutoConfiguration.java            (新增)
└── src/main/resources/
    └── META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports  (更新)
```

### 2.3 依赖变更

**`cartisan-data-jpa/build.gradle.kts`**:
```kotlin
dependencies {
    // 现有依赖...

    // Druid Spring Boot 3 Starter
    compileOnly("com.alibaba:druid-spring-boot-3-starter:1.2.23")
}
```

使用 `compileOnly` 的原因：
- 业务项目显式配置 `spring.datasource.type=DruidDataSource` 时才生效
- 避免强制替换 Spring Boot 默认的 HikariCP
- 业务可自由选择数据源实现

### 2.4 自动配置类

**`DruidAutoConfiguration.java`**:
```java
@AutoConfiguration
@ConditionalOnClass(DruidDataSource.class)
@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "com.alibaba.druid.pool.DruidDataSource")
@EnableConfigurationProperties(SpringDataSource.class)
public class DruidAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.datasource.druid.filter.stat.enabled", havingValue = "true", matchIfMissing = true)
    public StatFilter statFilter() {
        StatFilter filter = new StatFilter();
        // 默认配置
        return filter;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.datasource.druid.filter.wall.enabled", havingValue = "true", matchIfMissing = true)
    public WallFilter wallFilter() {
        WallFilter filter = new WallFilter();
        filter.setConfig(wallConfig());
        return filter;
    }

    @Bean
    public WallConfig wallConfig() {
        WallConfig config = new WallConfig();
        // 默认允许批量执行
        config.setMultiStatementAllow(true);
        return config;
    }
}
```

### 2.5 自动配置注册

**`AutoConfiguration.imports`**:
```
com.cartisan.data.jpa.config.CartisanDataJpaAutoConfiguration
com.cartisan.data.jpa.config.DruidAutoConfiguration
```

---

## 三、使用方式

### 3.1 最小配置

业务项目 `application.yml`:
```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    url: jdbc:postgresql://localhost:5432/mydb
    username: user
    password: pass
```

### 3.2 完整配置示例

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      # 连接池配置
      initial-size: 5
      max-active: 20
      min-idle: 5
      max-wait: 60000

      # 监控页面
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        login-username: admin
        login-password: admin

      # Web 监控
      web-stat-filter:
        enabled: true
        url-pattern: /*
        exclusions: "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*"

      # SQL 监控
      filter:
        stat:
          enabled: true
          log-slow-sql: true
          slow-sql-millis: 1000
          merge-sql: true
        wall:
          enabled: true
          config:
            multi-statement-allow: true
```

### 3.3 访问监控页面

启动后访问: `http://localhost:8080/druid/index.html`

---

## 四、实现清单

| 序号 | 任务 | 文件 |
|------|------|------|
| 1 | 添加 Druid Starter 依赖 | `cartisan-data-jpa/build.gradle.kts` |
| 2 | 创建 DruidAutoConfiguration | `cartisan-data-jpa/src/.../config/DruidAutoConfiguration.java` |
| 3 | 注册自动配置 | `AutoConfiguration.imports` |
| 4 | 编写单元测试 | `DruidAutoConfigurationTest.java` |
| 5 | 更新使用手册 | `docs/guide/cartisan-boot-使用手册.md` |
| 6 | 更新依赖说明 | 使用手册中的依赖章节 |

---

## 五、测试策略

### 5.1 单元测试

- 验证 `@ConditionalOnProperty` 正确触发
- 验证 Filter Bean 正确创建
- 验证默认配置值

### 5.2 集成测试

- 验证配置 HikariCP 时不加载 Druid 配置
- 验证配置 Druid 时正确加载 Filter
- 验证监控页面可访问

---

## 六、注意事项

1. **版本兼容**: 使用 `druid-spring-boot-3-starter` 兼容 Spring Boot 3.x
2. **默认行为**: 框架不强制使用 Druid，需业务显式配置 `spring.datasource.type`
3. **配置覆盖**: 业务可通过 `spring.datasource.druid.*` 覆盖所有默认配置
4. **安全性**: 生产环境建议配置监控页面登录密码

---

## 七、参考资料

- [Druid GitHub](https://github.com/alibaba/druid)
- [Druid Spring Boot Starter 文档](https://github.com/alibaba/druid/tree/master/druid-spring-boot-starter)
