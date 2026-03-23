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

    // cartisan-security - 可选依赖（编译期需要，运行时由使用方提供）
    // 用于 JooqTenantSupport 访问 TenantContext
    compileOnly(project(":cartisan-security"))

    // 测试时需要完整的 cartisan-security（用于 TenantContext 测试）
    testImplementation(project(":cartisan-security"))

    // 配置属性元数据处理器（IDE 自动补全提示）
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.4.0")

    // Lombok（编译时生效，不传递给使用者）
    compileOnly("org.projectlombok:lombok")
    annotationProcessor(platform(project(":cartisan-dependencies")))
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation(project(":cartisan-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
    options.compilerArgs.add("--enable-preview")
}
