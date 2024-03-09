plugins {
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":analyzer-commons"))
    implementation(libs.okttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization.converter)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.swing)
    testImplementation(libs.retrofit.mock)
}

tasks.test {
    loadProjectEnvironmentVariables().forEach(environment::put)
}

tasks.create<JavaExec>("run") {
    group = "Application"
    description = "Runs the Kotlin client application based on direct style, i.e. coroutines."
    loadProjectEnvironmentVariables().forEach(environment::put)
    mainClass.set("io.github.tassiLuca.analyzer.client.LauncherKt")
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
