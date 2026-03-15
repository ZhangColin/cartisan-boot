plugins {
    java
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

dependencies {
    // Platform - versions managed by cartisan-dependencies
    api(platform(project(":cartisan-dependencies")))

    // cartisan-web - 复用 PageResponse<T>（api 声明，传递给使用者）
    api(project(":cartisan-web"))

    // jOOQ Core
    implementation("org.jooq:jooq")

    // Spring Boot AutoConfiguration 支持
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    // 配置属性元数据处理器（IDE 自动补全提示）
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.4.0")

    // Testing
    testImplementation(project(":cartisan-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    // PostgreSQL JDBC 驱动（集成测试需要）
    testImplementation("org.postgresql:postgresql")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
