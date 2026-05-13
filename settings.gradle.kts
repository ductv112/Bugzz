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
        // mavenLocal() seeded with manually-downloaded artifacts for deps unreachable due to
        // host SSL-revocation-check failure (Plan 07-01 Task 1 Rule 3 auto-fix; Windows schannel
        // CRYPT_E_NO_REVOCATION_CHECK on freshly-issued Google CA certs).
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "Bugzz"
include(":app")
