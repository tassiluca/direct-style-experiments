dependencies {
    implementation(libs.sttp)
    implementation(libs.sttp.upickle)
    implementation(libs.scalamock)
    implementation(libs.scalamock.test)
    implementation(project(":analyzer-commons"))
}

tasks.test {
    loadProjectEnvironmentVariables().forEach(environment::put)
}

tasks.create<JavaExec>("run") {
    group = "Application"
    description = "Runs the Scala client application based on direct style, i.e. Gears."
    loadProjectEnvironmentVariables().forEach(environment::put)
    mainClass.set("io.github.tassiLuca.analyzer.client.directAnalyzerLauncher")
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
