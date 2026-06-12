plugins {
    alias(libs.plugins.android.library)
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
}

android {
    namespace = "io.senovative.liveness.sdk"
    compileSdk = Integer.parseInt(libs.versions.compile.get())

    defaultConfig {
        minSdk = Integer.parseInt(libs.versions.minimum.get())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES.txt"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/license.txt"
        }
    }
    
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    api(libs.androidx.core.ktx)

    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.material3)
    api(libs.androidx.activity.compose)
    api(libs.androidx.ui)
    api(libs.androidx.ui.graphics)
    api(libs.androidx.foundation)

    api(libs.camera.core)
    api(libs.camera.camera2)
    api(libs.camera.lifecycle)
    api(libs.camera.view)
    api(libs.mlkit.face.detection)
    api(libs.exifinterface)

    testImplementation(libs.junit)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "io.senovative.liveness"
                artifactId = "liveness-sdk"
                version = "1.0.0"
            }
        }
    }
}
