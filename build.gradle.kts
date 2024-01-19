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
        apply(plugin = scalatest.get().pluginId)
        apply(plugin = kotlin.jvm.get().pluginId)
        apply(plugin = dokka.get().pluginId)
        apply(plugin = kotlin.qa.get().pluginId)
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        with(rootProject.libs) {
            implementation(kotlin.stdlib)
            implementation(scala.stdlib)
            // Gears is a wip strawman library for async programming not already available on Maven Central...
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
}
