plugins {
    java
}

dependencies {
    // Platform - versions managed by cartisan-dependencies
    api(platform(project(":cartisan-dependencies")))

    // cartisan-core - provides AggregateRoot interface
    api(project(":cartisan-core"))

    // cartisan-event - provides DomainEventPublisher
    api(project(":cartisan-event"))

    // Spring Data JPA - provides JpaRepository, JpaSpecificationExecutor
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.h2database:h2")
    testImplementation("com.alibaba:druid-spring-boot-3-starter:1.2.23")

    // Lombok（编译时生效，不传递给使用者）
    compileOnly("org.projectlombok:lombok")
    annotationProcessor(platform(project(":cartisan-dependencies")))
    annotationProcessor("org.projectlombok:lombok")

    // Druid Spring Boot 3 Starter（可选依赖，业务需显式配置 spring.datasource.type 才生效）
    compileOnly("com.alibaba:druid-spring-boot-3-starter:1.2.23")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}

tasks.withType<Test> {
    useJUnitPlatform()
}
