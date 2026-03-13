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

    // Spring Boot Test 支持（@TestConfiguration 等注解）
    api("org.springframework.boot:spring-boot-test:3.4.0")

    // Spring Boot Test（MockMvc、@AutoConfigureMockMvc 等）
    api("org.springframework.boot:spring-boot-starter-test:3.4.0")

    // Spring Boot Testcontainers 支持（@ServiceConnection）
    api("org.springframework.boot:spring-boot-testcontainers:3.4.0")

    // Spring 依赖（规则类需要引用注解，测试 fixtures 需要）
    implementation("org.springframework.boot:spring-boot-starter-web:3.4.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.4.0")
    implementation("org.springframework.boot:spring-boot-starter-data-redis:3.4.0")
}
