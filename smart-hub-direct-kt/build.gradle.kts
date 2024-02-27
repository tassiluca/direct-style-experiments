plugins {
    application
}

dependencies {
    implementation(libs.kotlinx.coroutines.swing)
}

application {
    mainClass.set("io.github.tassiLuca.hub.LauncherKt")
}
