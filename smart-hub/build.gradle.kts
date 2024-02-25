dependencies {
    api(project(":commons"))
    api(project(":rears"))
    implementation(libs.kotlinx.coroutines.swing)
}

tasks.create<JavaExec>("runScala") {
    mainClass.set("io.github.tassiLuca.hub.launchUIMockedHub")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.create<JavaExec>("runKotlin") {
    mainClass.set("io.github.tassiLuca.hubkt.LauncherKt")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
