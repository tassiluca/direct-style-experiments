plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "1.1.18"
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
    ":blog-core-ws",
    ":rears-core",
    ":smart-home",
    ":back-and-forth",
)
