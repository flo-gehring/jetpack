import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("java")
    id("maven-publish")
}

group = "de.friendlyhedgehog"
version = "0.3-SNAPSHOT"
repositories {
    mavenCentral()
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }
}


publishing {

    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])
            groupId = "de.friendlyhedgehog"
            artifactId = "jetpack"
            version = "0.3.0-SNAPSHOT"

            pom {
                name.set("Jetpack")
                description.set("A little, packrat inspired parser.")
                url.set("https://github.com/flo-gehring/jetpack")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("flo-gehring")
                        name.set("Florian Gehring")
                        email.set("fg_secondary@protonmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/flo-gehring/jetpack.git")
                    developerConnection.set("scm:git:ssh://github.com:flo-gehring/jetpack.git")
                    url.set("https://github.com/flo-gehring/jetpack")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GithubPackages"
            url = uri("https://maven.pkg.github.com/flo-gehring/jetpack")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    testCompileOnly("org.projectlombok:lombok:1.18.36")
    testImplementation("org.assertj:assertj-core:3.11.1")
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