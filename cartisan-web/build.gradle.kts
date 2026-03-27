plugins {
    java
}

dependencies {
    // Platform - versions managed by cartisan-dependencies
    api(platform(project(":cartisan-dependencies")))

    implementation(project(":cartisan-core"))

    // Spring Web - provides @ControllerAdvice, @ExceptionHandler, ResponseEntity
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Spring AOP - provides @Aspect, @Around, ProceedingJoinPoint
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Spring Validation - provides Bean Validation, @Valid, @Validated
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Spring Data Redis - provides StringRedisTemplate for distributed lock
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Fastjson2 - JSON serialization
    implementation("com.alibaba.fastjson2:fastjson2")

    // MapStruct - 类型安全的对象映射
    implementation("org.mapstruct:mapstruct")

    // Testing - provides MockMvc, @SpringBootTest, @AutoConfigureMockMvc
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")

    // Lombok（编译时生效，不传递给使用者）
    compileOnly("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    annotationProcessor(platform(project(":cartisan-dependencies")))
    annotationProcessor("org.projectlombok:lombok")

    // MapStruct 注解处理器（必须在 Lombok 之后）
    annotationProcessor("org.mapstruct:mapstruct-processor")

    // Lombok + MapStruct 集成（必须在 mapstruct-processor 之后）
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // Test annotation processors（测试源码也需要注解处理器）
    testAnnotationProcessor(platform(project(":cartisan-dependencies")))
    testAnnotationProcessor("org.projectlombok:lombok")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor")
    testAnnotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
