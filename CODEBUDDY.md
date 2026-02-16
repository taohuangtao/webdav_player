# CODEBUDDY.md This file provides guidance to CodeBuddy when working with code in this repository.

## Project Overview

WebDAVViewer is an Android application for browsing and viewing WebDAV resources (files, videos, images). Built with Kotlin, Jetpack Compose, and Material3.

## Build Commands

```bash
# Build the project
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Run unit tests
./gradlew test

# Run Android instrumented tests
./gradlew connectedAndroidTest

# Lint check
./gradlew lint

# Gradle sync (after dependency changes)
./gradlew build --refresh-dependencies
```

## Architecture

The project follows **Clean Architecture** with MVVM pattern:

```
┌─────────────────────────────────────┐
│  UI Layer (Jetpack Compose)          │
│  - browser/, player/, viewer/,       │
│    settings/, theme/                 │
└─────────────────────────────────────┘
                ↓
┌─────────────────────────────────────┐
│  ViewModel Layer                     │
│  - StateFlow for UI state            │
│  - Hilt injection                    │
└─────────────────────────────────────┘
                ↓
┌─────────────────────────────────────┐
│  Repository Layer                    │
│  - WebDAVRepository                  │
│  - ConfigRepository                  │
└─────────────────────────────────────┘
                ↓
┌─────────────────────────────────────┐
│  Data Layer                          │
│  - WebDAVClient (remote/)            │
│  - ConfigDataStore (local/)          │
│  - Models (data/model/)              │
└─────────────────────────────────────┘
```

### Package Structure

```
com.tdull.webdavviewer.app/
├── data/
│   ├── model/          # Data classes: WebDAVResource, ServerConfig, WebDAVException
│   ├── remote/         # WebDAVClient (OkHttp-based PROPFIND requests)
│   ├── local/          # ConfigDataStore (SharedPreferences/DataStore)
│   └── repository/     # WebDAVRepository interface and implementation
├── di/                 # Hilt modules: WebDAVModule
├── ui/                 # Compose screens: browser/, player/, viewer/, settings/, theme/
├── viewmodel/          # ViewModels for each screen
├── navigation/         # Navigation graph and screen definitions
├── MainActivity.kt     # Single Activity with @AndroidEntryPoint
└── WebDavApplication.kt # Hilt application class
```

### Key Technologies

- **WebDAV Protocol**: Custom OkHttp client sending PROPFIND requests with XML parsing
- **Dependency Injection**: Hilt with `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`
- **UI**: Jetpack Compose with Material3, Navigation Compose
- **Media**: ExoPlayer (Media3) for video streaming, Coil for image loading
- **State Management**: StateFlow + ViewModel, sealed classes for UI state
- **Async**: Kotlin Coroutines, suspend functions

## WebDAV Implementation

The `WebDAVClient` class handles all WebDAV operations:

1. **PROPFIND Requests**: Sends XML body to list directory contents
2. **XML Parsing**: Uses Android's XmlPullParser for multistatus responses
3. **Authentication**: HTTP Basic Auth via OkHttp credentials
4. **URL Encoding**: Custom path encoding for special characters in filenames
5. **Resource Type Detection**: MIME type and extension-based classification (VIDEO/IMAGE/AUDIO/OTHER)

### WebDAVResource Model

```kotlin
data class WebDAVResource(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val contentType: String?,
    val resourceType: ResourceType // DIRECTORY, VIDEO, IMAGE, AUDIO, OTHER
)
```

## Development Guidelines

### When Adding New Features

1. **Data Layer First**: Create models in `data/model/`, then repository methods
2. **Repository Pattern**: All data access goes through repository interfaces
3. **Dependency Injection**: Register new dependencies in `di/` modules
4. **ViewModel**: Use `@HiltViewModel` and expose state via `StateFlow`
5. **Compose UI**: Keep screens stateless, handle navigation via callbacks

### Testing WebDAV Features

When testing WebDAV connectivity:
- Use `testConnection(config: ServerConfig)` before operations
- Handle `WebDAVException` sealed class (ConnectionFailed, AuthenticationFailed, ResourceNotFound, InvalidResponse)
- Result wrapper pattern: `Result<T>` for error handling

### Media Playback

- Video: Use `getStreamUrl(path)` to get direct HTTP URL for ExoPlayer
- Images: Load via Coil with WebDAV authentication headers if needed
- Stream directly, don't download entire files

## Dependencies

All dependencies are managed in `app/build.gradle.kts`:
- Compose BOM 2024.02.00
- Navigation Compose 2.7.6
- Hilt 2.50
- OkHttp 4.12.0
- Media3 ExoPlayer 1.2.1
- Coil 2.5.0
- DataStore 1.0.0
- Kotlin Coroutines 1.7.3

## Android Manifest

- `android:usesCleartextTraffic="true"` enabled for HTTP WebDAV servers
- INTERNET and ACCESS_NETWORK_STATE permissions
- Single MainActivity with launch intent

## Current Development Status

Refer to `android_webdav_dev_steps.md` for detailed step-by-step development plan. Core data layer is implemented; UI screens and navigation are in progress.
