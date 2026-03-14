plugins {
    java
}

dependencies {
    // Platform - versions managed by cartisan-dependencies
    api(platform(project(":cartisan-dependencies")))

    implementation(project(":cartisan-core"))

    // Spring Web - provides @ControllerAdvice, @ExceptionHandler, ResponseEntity
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Spring Validation - provides Bean Validation, @Valid, @Validated
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Testing - provides MockMvc, @SpringBootTest, @AutoConfigureMockMvc
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
