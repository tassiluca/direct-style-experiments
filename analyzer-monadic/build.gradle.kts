plugins {
    kotlin("plugin.serialization") version "1.9.22"
}

dependencies {
    // Scala
    implementation(libs.sttp)
    implementation(libs.sttp.upickle)
    implementation("org.typelevel:cats-core_3:2.10.0")
    // Kotlin
    implementation(libs.okttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.adapter.rxjava)
    implementation(libs.retrofit.serialization.converter)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.retrofit.mock)
}
