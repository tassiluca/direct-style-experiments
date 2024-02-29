plugins {
    application
}

dependencies {
    implementation(project(":rears"))
}

application {
    mainClass.set("io.github.tassiLuca.hub.launchUIMockedHub")
}
