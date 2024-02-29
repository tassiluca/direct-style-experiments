import org.gradle.api.tasks.testing.logging.TestLogEvent

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    scala
    alias(libs.plugins.scalatest)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.qa)
}

val projectRepository = projectDir.resolve("libs")

allprojects {

    with(rootProject.libs.plugins) {
        apply(plugin = "scala")
        apply(plugin = kotlin.jvm.get().pluginId)
        apply(plugin = dokka.get().pluginId)
        apply(plugin = kotlin.qa.get().pluginId)
    }

    if (!this.name.contains("kt")) {
        apply(plugin = rootProject.libs.plugins.scalatest.get().pluginId)
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        with(rootProject.libs) {
            implementation(kotlin.stdlib)
            implementation(scala.stdlib)
            // Gears is a wip strawman library for async programming not available on Maven Central: it is
            // included as git submodule in this project and added as dependency via jars.
            val gears = "gears_3-0.1.0-SNAPSHOT"
            implementation(
                files(
                    listOf(
                        "$gears.jar",
                        "$gears-javadoc.jar",
                        "$gears-sources.jar",
                    ).map { projectRepository.resolve(it) },
                ),
            )
            implementation(kotlinx.coroutines.core)
            testRuntimeOnly(flexmark) // needed to make it works scalatest
            testImplementation(scalatest)
            testImplementation(bundles.kotlin.testing)
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    kotlin {
        target {
            compilations.all {
                kotlinOptions {
                    allWarningsAsErrors = true
                    freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
                }
            }
        }
    }

    tasks.test {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
            showCauses = true
            showStackTraces = true
            events(*TestLogEvent.values())
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    tasks.jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
