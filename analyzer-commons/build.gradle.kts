dependencies {
    implementation(libs.sttp)
    implementation(libs.sttp.upickle)
    api(project(":commons"))
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
