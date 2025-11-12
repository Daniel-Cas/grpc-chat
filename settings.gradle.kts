pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }

    versionCatalogs {
        create("libraries") {
            from(files("gradle/lib.versions.toml"))
        }
        create("awssdk") {
            from("aws.sdk.kotlin:version-catalog:1.4.37")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "grcp-chat"