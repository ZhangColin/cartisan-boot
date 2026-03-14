allprojects {
    group = "com.cartisan"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    // cartisan-dependencies uses java-platform plugin, not java-library
    if (project.name != "cartisan-dependencies") {
        apply(plugin = "java-library")
        apply(plugin = "jacoco")
    }

    // Only configure Java toolchain for java-library projects
    if (project.name != "cartisan-dependencies") {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(21)
            }
        }
    }

    // Only configure test dependencies for java-library projects
    if (project.name != "cartisan-dependencies") {
        dependencies {
            "testImplementation"(platform("org.junit:junit-bom:5.11.4"))
            "testImplementation"("org.junit.jupiter:junit-jupiter")
            "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }

        // Jacoco 配置
        tasks.named<JacocoReport>("jacocoTestReport") {
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }
    }
}
