rootProject.name = "Hmlkbi"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
include(":core:network")
project(":core:network").projectDir = file("core/network")
include(":core:model")
include(":feature:service:data")
include(":feature:service:domain")
include(":core:di")
project(":core:di").projectDir = file("core/di")
include(":androidapp")
include(":feature:service:ui")
include(":core:designsystem")
