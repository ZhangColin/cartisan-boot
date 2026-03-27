plugins {
    `java-platform`
}

dependencies {
    // Spring Boot BOM - manages all Spring Boot starter versions
    api(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))

    // SpringDoc OpenAPI - Web 通用文档工具
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    // Flyway - 数据库迁移（与 JPA 配套）
    api("org.flywaydb:flyway-core:10.18.0")

    // jOOQ BOM（F04-01）
    api(platform("org.jooq:jooq-bom:3.19.29"))

    // Sa-Token（F03-01）- 直接用 api() 声明带版本约束
    api("cn.dev33:sa-token-spring-boot3-starter:1.45.0")

    // Lombok - 框架内置，减少样板代码
    api("org.projectlombok:lombok:1.18.34")

    // MapStruct - 类型安全的对象映射
    api("org.mapstruct:mapstruct:1.6.3")
    api("org.mapstruct:mapstruct-processor:1.6.3")

    // Lombok + MapStruct 集成
    api("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // AssertJ - 断言库
    api("org.assertj:assertj-core:3.26.3")

    // ArchUnit - 架构测试
    api("com.tngtech.archunit:archunit:1.3.0")
    api("com.tngtech.archunit:archunit-junit5:1.3.0")

    // WireMock - API 测试模拟
    api("org.wiremock.integrations:wiremock-spring-boot:3.2.0")

    // Fastjson2 - JSON 序列化
    api("com.alibaba.fastjson2:fastjson2:2.0.53")

    // JUnit BOM - 测试框架
    api(platform("org.junit:junit-bom:5.11.4"))

    // Hutool - Java 工具类库
    api("cn.hutool:hutool-all:5.8.34")

    // PIT - 变异测试插件
    api("org.pitest:pitest-junit5-plugin:1.2.3")

    // Druid - 数据库连接池（可选）
    api("com.alibaba:druid-spring-boot-3-starter:1.2.23")
}

javaPlatform {
    allowDependencies()
}
