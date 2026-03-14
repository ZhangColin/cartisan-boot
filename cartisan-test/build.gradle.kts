plugins {
    java
    `java-library`
}

dependencies {
    implementation(project(":cartisan-core"))

    // 测试框架（api 配置暴露给业务项目）
    api(platform(libs.junit.bom))
    api(libs.junit.jupiter)
    api(libs.assertj.core)
    api(libs.mockito.core)
    api(libs.archunit.junit5)

    // ArchUnit 编译依赖（本模块需要编译）
    implementation(libs.archunit.junit5)

    // Testcontainers
    api(platform(libs.testcontainers.bom))
    api(libs.testcontainers.core)
    api(libs.testcontainers.postgresql)
    testImplementation("org.testcontainers:junit-jupiter")

    // Docker Java API（显式声明以确保兼容性）
    testImplementation("com.github.docker-java:docker-java:3.4.0")

    // Spring Boot Test 支持（@TestConfiguration 等注解）
    api("org.springframework.boot:spring-boot-test:3.4.0")

    // Spring Test（MockMvc、ResultActions、RequestPostProcessor）
    api("org.springframework:spring-test:6.2.0")
    implementation("org.springframework:spring-test:6.2.0")

    // Spring Boot Test（MockMvc、@AutoConfigureMockMvc 等）
    // api 配置不会对本模块 main 代码编译可用，需要 implementation
    api("org.springframework.boot:spring-boot-starter-test:3.4.0")
    implementation("org.springframework.boot:spring-boot-starter-test:3.4.0")

    // Spring Boot Testcontainers 支持（@ServiceConnection）
    api("org.springframework.boot:spring-boot-testcontainers:3.4.0")

    // Spring 依赖（规则类需要引用注解，测试 fixtures 需要）
    implementation("org.springframework.boot:spring-boot-starter-web:3.4.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.4.0")
    implementation("org.springframework.boot:spring-boot-starter-data-redis:3.4.0")

    // Jackson（JSON 序列化，ApiTestAssertions 需要）
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // PostgreSQL JDBC 驱动（Testcontainers 需要实际驱动连接数据库）
    runtimeOnly("org.postgresql:postgresql:42.7.4")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("org.testcontainers.disableRyuk", "true")
    systemProperty("DOCKER_HOST", "unix:///Users/zhangcolin/.docker/run/docker.sock")
    environment("DOCKER_HOST", "unix:///Users/zhangcolin/.docker/run/docker.sock")
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}
