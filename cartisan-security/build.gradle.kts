plugins {
    java
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

dependencies {
    // Platform - versions managed by cartisan-dependencies
    api(platform(project(":cartisan-dependencies")))

    // 依赖 cartisan-core（基础类型）
    api(project(":cartisan-core"))

    // 依赖 cartisan-web（ApiResponse、GlobalExceptionHandler、RequestContext）
    api(project(":cartisan-web"))

    // Sa-Token（版本由 BOM 管理，不写版本号）
    implementation("cn.dev33:sa-token-spring-boot3-starter")

    // 测试依赖（spring-boot-starter-test 已包含 JUnit 5 + AssertJ）
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
