pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // RESILIENCE FALLBACK (run 41618048112, 2026-11-03): Maven Central returned
        // 429/5xx on the cloud runner and onnxruntime-android 1.20.0 +
        // auto-value-annotations 1.6.3 could not be resolved — while the ADJACENT
        // runs (41617910340) resolved them fine. Gradle tries repositories in
        // declaration order and falls through when a repo cannot serve an
        // artifact, so the primary authorities keep priority and this mirror only
        // serves what they could not. Same remedy as the sandbox init script
        // (scripts/gradle-mirrors.init.gradle). Artifacts are byte-identical and
        // still integrity-checked by Gradle checksum verification.
        // CP-G4.9-003: authoritative host of the sherpa-onnx Android AAR
        // (com.github.k2-fsa only; scoped so nothing else can be served from here).
        maven {
            url = uri("https://jitpack.io")
            content { includeGroup("com.github.k2-fsa") }
        }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
    }
}

rootProject.name = "HermesVoiceConsole"
include(":app")
