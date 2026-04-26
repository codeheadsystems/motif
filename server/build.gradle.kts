plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // Pin Jetty >= 12.1.6 to override 12.1.5 pulled by Dropwizard 5.0.1.
    // Both BOMs are needed because Dropwizard pulls jetty-* and jetty-ee10-* families.
    implementation(platform(libs.jetty.bom))
    implementation(platform(libs.jetty.ee10.bom))
    testImplementation(platform(libs.jetty.bom))
    testImplementation(platform(libs.jetty.ee10.bom))

    implementation(project(":common"))
    implementation(project(":server-db"))
    implementation(libs.bundles.core)
    implementation(libs.bundles.dropwizard)
    implementation(libs.hofmann.dropwizard)

    // DB (needed for SetupBundle JDBI initialization)
    implementation(libs.bundles.db)

    // DI
    implementation(libs.dagger)
    implementation(libs.jsr305)
    annotationProcessor(libs.dagger.compiler)

    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation(libs.hofmann.client)
    testImplementation(libs.dropwizard.testing)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    constraints {
        // Pin Bouncy Castle >= 1.84 to override the 1.83 pulled transitively by
        // hofmann-rfc 1.3.2 (via hofmann-server -> hofmann-dropwizard).
        implementation(libs.bouncycastle.bcprov) {
            because("hofmann-rfc 1.3.2 pulls bcprov-jdk18on 1.83; we require >= 1.84")
        }
        testImplementation(libs.bouncycastle.bcprov) {
            because("hofmann-rfc 1.3.2 pulls bcprov-jdk18on 1.83; we require >= 1.84")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "com.codeheadsystems.motif.server.MotifApplication"
}

val buildWebapp by tasks.registering(Exec::class) {
    workingDir = file("../webapp")
    commandLine("npm", "run", "build")
}

tasks.named("processResources") {
    dependsOn(buildWebapp)
}

val testWebapp by tasks.registering(Exec::class) {
    workingDir = file("../webapp")
    commandLine("npm", "test")
}

tasks.named<Test>("test") {
    dependsOn(testWebapp)
    useJUnitPlatform()
}
