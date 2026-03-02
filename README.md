# WebDAVViewer

一个用于浏览和查看 WebDAV 资源（文件、视频、图片）的 Android 应用程序。

## 项目概述

WebDAVViewer 使用 Kotlin、Jetpack Compose 和 Material3 构建，遵循 Clean Architecture 和 MVVM 模式。主要功能包括：

- **WebDAV 资源浏览**：支持浏览远程 WebDAV 服务器上的文件和目录
- **多媒体播放**：使用 ExoPlayer 播放视频，Coil 加载图片
- **服务器配置管理**：支持多个 WebDAV 服务器配置和连接
- **收藏功能**：收藏常用文件和目录

### 核心技术

- **UI**：Jetpack Compose + Material3 + Navigation Compose
- **依赖注入**：Hilt
- **网络**：OkHttp + WebDAV PROPFIND 协议
- **媒体**：Media3 ExoPlayer + Coil
- **状态管理**：StateFlow + ViewModel
- **异步**：Kotlin Coroutines
- **数据存储**：DataStore

## 构建命令

### 构建项目

```bash
# 构建 Debug APK
gradlew assembleDebug

# 构建 Release APK
gradlew assembleRelease

# 清理构建
gradlew clean
```

### 测试

```bash
# 运行单元测试
gradlew test

# 运行 Android 设备测试
gradlew connectedAndroidTest
```

### 其他

```bash
# Lint 检查
gradlew lint

# Gradle 同步（依赖更改后）
gradlew build --refresh-dependencies
```

## 环境要求

- Android SDK
- JDK 11+
- Gradle 8.0+

## 许可证

本项目仅供学习和个人使用。
