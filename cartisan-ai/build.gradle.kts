plugins { java }

dependencies {
    // 版本 BOM：cartisan-dependencies 内嵌 spring-boot-dependencies，
    // spring-webflux 等 Spring 组件版本均由此管理，无需手写版本号
    api(platform(project(":cartisan-dependencies")))

    // CartisanException 等基础类型出现在公开 API，下游可见
    api(project(":cartisan-core"))

    // Flux<ChatStreamEvent> 是公开 SPI 返回类型，下游可见；
    // 选 spring-webflux 而非 spring-boot-starter-webflux，避免引入 Netty 嵌入式服务器
    api("org.springframework:spring-webflux")

    // SSE 支持：SseEmitter 位于 spring-webmvc
    api("org.springframework:spring-webmvc")

    // AutoConfiguration 基础设施，内部使用
    implementation("org.springframework.boot:spring-boot-starter")

    // Reactor Netty HTTP — WebClient 的 HTTP/1.1 connector 需要直接引用
    implementation("io.projectreactor.netty:reactor-netty-http")

    // OpenAI DTO 序列化：@JsonNaming / @JsonIgnoreProperties / @JsonInclude
    implementation("com.fasterxml.jackson.core:jackson-databind")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.wiremock.integrations:wiremock-spring-boot:3.2.0")
    testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
