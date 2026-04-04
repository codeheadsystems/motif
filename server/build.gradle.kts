plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation(project(":server-db"))
    implementation(libs.bundles.core)
    implementation(libs.bundles.dropwizard)

    // DI
    implementation(libs.dagger)
    implementation(libs.jsr305)
    annotationProcessor(libs.dagger.compiler)

    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation(libs.dropwizard.testing)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "com.codeheadsystems.motif.server.MotifApplication"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
