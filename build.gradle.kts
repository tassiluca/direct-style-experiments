@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    scala
    alias(libs.plugins.scalatest)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.qa)
    application
}

val projectRepository = projectDir.resolve("libs")

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.scala.stdlib)
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
    testRuntimeOnly(libs.flexmark) // needed to make it works scalatest
    testImplementation(libs.scalatest)
    testImplementation(libs.bundles.kotlin.testing)
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
