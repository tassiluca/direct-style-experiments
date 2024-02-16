dependencies {
    implementation(libs.sttp)
    implementation(libs.sttp.upickle)
    implementation(libs.cats.core)
    api(project(":analyzer-commons"))
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
