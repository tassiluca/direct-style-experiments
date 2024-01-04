@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    scala
    alias(libs.plugins.scalatest)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.qa)
    application
}

repositories {
    mavenLocal() // used for gears dependency
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.scala.stdlib)
    implementation(libs.gears) // from maven local repository
    testRuntimeOnly(libs.flexmark) // needed to make it works scalatest
    testImplementation(libs.scalatest)
    testImplementation(libs.bundles.kotlin.testing)
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
