dependencies {
    implementation(libs.sttp)
    implementation(libs.sttp.upickle)
    implementation(libs.cats.core)
    implementation("com.softwaremill.sttp.client3:async-http-client-backend-monix_3:3.9.3")
    implementation("io.monix:monix_3:3.4.1")
    implementation(project(":analyzer-commons"))
}

tasks.test {
    loadProjectEnvironmentVariables().forEach(environment::put)
}

tasks.create<JavaExec>("run") {
    group = "Application"
    description = "Runs the client application."
    loadProjectEnvironmentVariables().forEach(environment::put)
    mainClass.set("io.github.tassiLuca.analyzer.client.monadicAnalyzerLauncher")
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
