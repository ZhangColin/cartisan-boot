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
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}

tasks.withType<Test> {
    useJUnitPlatform()
}
