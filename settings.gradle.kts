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

rootProject.name = "PPS-22-direct-style-experiments"
include(
    ":back-and-forth",
)
