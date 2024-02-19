plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.1"
}

gitHooks {
    preCommit {
        tasks("check")
    }
    commitMsg {
        conventionalCommits()
    }
    createHooks()
}

rootProject.name = "direct-style-experiments"
include(
    ":commons",
    ":blog-ws-commons",
    ":blog-ws-direct",
    ":blog-ws-monadic",
    ":rears-core",
    ":smart-home",
    ":back-and-forth",
    ":analyzer-commons",
    ":analyzer-monadic",
    ":analyzer-direct",
)
