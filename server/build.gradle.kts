plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}


repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    api(libs.bundles.core)
    testImplementation(libs.bundles.testing)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(libs.bundles.db)

    // DI
    implementation(libs.bundles.di)
    annotationProcessor(libs.dagger.compiler)

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
