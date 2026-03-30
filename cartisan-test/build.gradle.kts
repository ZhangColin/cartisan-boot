plugins {
    java
    `java-library`
}

dependencies {
    implementation(project(":cartisan-core"))

    // Platform - 版本由 cartisan-dependencies 统一管理
    api(platform(project(":cartisan-dependencies")))

    // 测试框架（api 配置暴露给业务项目）
    api("org.junit.jupiter:junit-jupiter")
    api("org.assertj:assertj-core")
    api("org.mockito:mockito-core")
    api("com.tngtech.archunit:archunit")
    api("com.tngtech.archunit:archunit-junit5")

    // Spring Boot Test 支持（@TestConfiguration 等注解）
    api("org.springframework.boot:spring-boot-test")

    // Spring Test（MockMvc、ResultActions、RequestPostProcessor）
    api("org.springframework:spring-test")

    // Spring Boot Test（MockMvc、@AutoConfigureMockMvc 等）
    api("org.springframework.boot:spring-boot-starter-test")

    // Spring 依赖（规则类需要引用注解，测试 fixtures 需要）
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Spring JDBC（IntegrationTestBase 需要 JdbcTemplate）
    implementation("org.springframework:spring-jdbc")

    // Jackson（JSON 序列化，ApiTestAssertions 需要）
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // Lombok（编译时生效，不传递给使用者）
    compileOnly("org.projectlombok:lombok")
    annotationProcessor(platform(project(":cartisan-dependencies")))
    annotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}
