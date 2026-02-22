import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("java")
}

group = "de.flo-gehring"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    testCompileOnly("org.projectlombok:lombok:1.18.36")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.36")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    maxHeapSize = "1G"
    testLogging {
        events(TestLogEvent.FAILED)
        events(TestLogEvent.SKIPPED)
    }
}