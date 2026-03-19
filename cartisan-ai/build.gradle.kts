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

    // AutoConfiguration 基础设施，内部使用
    implementation("org.springframework.boot:spring-boot-starter")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
