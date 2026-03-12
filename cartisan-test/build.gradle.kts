dependencies {
    implementation(project(":cartisan-core"))

    api(platform("org.junit:junit-bom:5.11.4"))
    api("org.junit.jupiter:junit-jupiter")
    api("org.junit.jupiter:junit-jupiter-api")
    api("org.assertj:assertj-core:3.27.3")
    api("org.mockito:mockito-core:5.15.2")
    api("com.tngtech.archunit:archunit-junit5:1.3.0")
}