dependencies {
    implementation(libs.sttp)
    implementation(libs.sttp.upickle)
    implementation(libs.cats.core)
    implementation("com.softwaremill.sttp.client3:async-http-client-backend-monix_3:3.9.3")
    implementation("io.monix:monix_3:3.4.1")
    api(project(":analyzer-commons"))
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
