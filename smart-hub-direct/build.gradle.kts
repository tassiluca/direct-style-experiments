plugins {
    application
}

dependencies {
    api(project(":commons"))
    api(project(":rears"))
}

application {
    mainClass.set("io.github.tassiLuca.hub.launchUIMockedHub")
}
