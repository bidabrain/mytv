pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // GeckoView 发布在 Mozilla 自己的 Maven 仓库（不在 Maven Central）。
        maven { url = uri("https://maven.mozilla.org/maven2") }
    }
}

rootProject.name = "MyTvLive"
include(":app")
