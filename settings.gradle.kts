import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    repositories {
        // 华为云 Maven 镜像（与 Snishaper-Droid 一致：可同时代理 Google Maven、
        // Maven Central、Gradle Plugin Portal；官方源兜底，CI 上会切回官方源）。
        maven("https://repo.huaweicloud.com/repository/maven/")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://repo.huaweicloud.com/repository/maven/")
        google()
        mavenCentral()
    }
}

rootProject.name = "Snishaper-Droid-Native"

include(":app")

include(":core:model")
include(":core:common")
include(":core:design")
include(":core:engine")
include(":core:data")

include(":feature:dashboard")
include(":feature:connections")
include(":feature:rules")
include(":feature:dns")
include(":feature:proxies")
include(":feature:routing")
include(":feature:evolution")
include(":feature:logs")
include(":feature:profiles")
include(":feature:settings")
include(":feature:onboarding")
