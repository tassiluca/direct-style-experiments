plugins {
    application
}

dependencies {
    implementation(libs.kotlinx.coroutines.swing)
    implementation(project(":smart-hub-direct"))
}

application {
    mainClass.set("io.github.tassiLuca.hub.LauncherKt")
}
