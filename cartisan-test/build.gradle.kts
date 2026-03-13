dependencies {
    implementation(project(":cartisan-core"))

    api(platform(libs.junit.bom))
    api(libs.junit.jupiter)
    api(libs.assertj.core)
    api(libs.mockito.core)
    api(libs.archunit.junit5)
}
