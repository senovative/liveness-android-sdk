# Senovative Liveness SDK

The Senovative Liveness SDK provides a native Android solution for liveness detection and KTP OCR integration using Jetpack Compose.

## Prerequisites
- Minimum SDK: 24
- Compile/Target SDK: 37
- Kotlin 1.9.0+ / 2.0+

## Installation

The SDK is published to JitPack. To integrate it into your Android project, follow these steps:

### 1. Add JitPack Repository
Add JitPack to your root `settings.gradle.kts` (or root `build.gradle.kts` in older projects):

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add Dependency
Add the following dependency to your `app/build.gradle.kts` file:

```kotlin
dependencies {
    implementation("com.github.senovative:liveness-android-sdk:<version>")
}
```

## Usage

### Permissions
The SDK requires camera permissions. These are automatically merged into your app's `AndroidManifest.xml` from the SDK.
Ensure you request runtime permissions for `android.permission.CAMERA` before launching the liveness flow.

### Integration in Jetpack Compose
The SDK exposes composable screens and utilities for Liveness Detection. You can integrate `LivenessDetectionRoute` into your navigation graph or activity.

```kotlin
import io.senovative.liveness.sdk.view.LivenessDetectionRoute

// Inside your Compose Activity or Navigation Host
LivenessDetectionRoute(
    onResult = { result -> 
        if (result.isLive) {
            // Handle successful liveness check
            // e.g. show success message
        } else {
            // Handle failure or timeout
            // result.message contains error info
        }
    },
    onBack = {
        // Handle when user presses the close/back button
    }
)
```

## License
Specify your license here.
