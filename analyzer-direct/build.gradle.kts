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
    implementation(libs.retrofit.serialization.converter)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.swing)
    testImplementation(libs.retrofit.mock)
    api(project(":analyzer-commons"))
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    loadProjectEnvironmentVariables().forEach(environment::put)
}

tasks.create<JavaExec>("runScala") {
    group = "Application"
    description = "Runs the Scala client application based on direct style, i.e. Gears."
    loadProjectEnvironmentVariables().forEach(environment::put)
    mainClass.set("io.github.tassiLuca.analyzer.client.directAnalyzerLauncher")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.create<JavaExec>("runKotlin") {
    group = "Application"
    description = "Runs the Kotlin client application based on direct style, i.e. coroutines."
    loadProjectEnvironmentVariables().forEach(environment::put)
    mainClass.set("io.github.tassiLuca.analyzerkt.client.LauncherKt")
    classpath = sourceSets["main"].runtimeClasspath
}

fun loadProjectEnvironmentVariables(): Map<String, String> {
    val envs = if (System.getenv().containsKey("GH_TOKEN")) {
        mapOf("GH_TOKEN" to System.getenv("GH_TOKEN"))
    } else {
        file(rootDir.path).resolveAll("analyzer-commons", ".env").loadEnvironmentVariables()
    }
    return envs.also {
        require(it.contains("GH_TOKEN")) {
            "`GH_TOKEN` env variable is required either via `.env` file (in analyzer-commons) or system environment."
        }
    }
}

fun File.resolveAll(vararg paths: String): File = paths.fold(this) { f, s -> f.resolve(s) }

fun File.loadEnvironmentVariables(): Map<String, String> = runCatching {
    readLines().associate {
        val (key, value) = it.split("=")
        key to value
    }
}.getOrElse { emptyMap() }
