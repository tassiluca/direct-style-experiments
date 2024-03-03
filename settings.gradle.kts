plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.1"
}

gitHooks {
    preCommit {
        tasks("test")
    }
    commitMsg {
        conventionalCommits()
    }
    createHooks(overwriteExisting = true)
}

rootProject.name = "direct-style-experiments"
include(
    ":commons",
    ":blog-ws-commons",
    ":blog-ws-monadic",
    ":blog-ws-direct",
    ":blog-ws-direct-kt",
    ":analyzer-commons",
    ":analyzer-monadic",
    ":analyzer-direct",
    ":analyzer-direct-kt",
    ":rears",
    ":smart-hub-direct",
    ":smart-hub-direct-kt",
)
