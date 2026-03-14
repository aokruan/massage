import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "ru.aokruan.androidapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.aokruan.androidapp"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    flavorDimensions += "env"
    productFlavors {
        create("dev") { dimension = "env"; buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"") }
        create("stage") { dimension = "env"; buildConfigField("String", "BASE_URL", "\"https://stage.api.example.com/\"") }
        create("prod") { dimension = "env"; buildConfigField("String", "BASE_URL", "\"https://api.example.com/\"") }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
}

dependencies {
    implementation(project(":composeApp")) // shared UI
    implementation(libs.androidx.activity.compose)
}