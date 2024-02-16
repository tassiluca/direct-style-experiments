plugins {
    kotlin("plugin.serialization") version "1.9.22"
}

dependencies {
    // Scala
    implementation(libs.sttp)
    implementation(libs.sttp.upickle)
    // Kotlin
    implementation(libs.okttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.adapter.rxjava)
    implementation(libs.retrofit.serialization.converter)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.retrofit.mock)
    api(project(":analyzer-commons"))
}
