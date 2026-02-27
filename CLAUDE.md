# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此代码库中工作时提供指导。

## 项目概述

WebDAVViewer 是一个用于浏览和查看 WebDAV 资源（文件、视频、图片）的 Android 应用程序。使用 Kotlin、Jetpack Compose 和 Material3 构建。

## 构建命令

```bash
# 构建项目
./gradlew assembleDebug

# 构建 release APK（输出：app/build/outputs/apk/release/WebDAVViewer-1.0-release.apk）
./gradlew assembleRelease

# 清理构建
./gradlew clean

# 运行单元测试
./gradlew test

# 运行 Android 设备测试
./gradlew connectedAndroidTest

# Lint 检查
./gradlew lint

# Gradle 同步（依赖更改后）
./gradlew build --refresh-dependencies
```

## 架构

项目遵循 **Clean Architecture** 和 MVVM 模式：

```
UI (Compose) → ViewModel → Repository → Data Layer
```

### 包结构

```
com.tdull.webdavviewer.app/
├── data/
│   ├── model/          # WebDAVResource, ServerConfig, WebDAVException
│   ├── remote/         # WebDAVClient（基于 OkHttp 的 PROPFIND 请求）
│   ├── local/          # ConfigDataStore
│   └── repository/     # Repository 接口和实现
├── di/                 # Hilt 模块
├── ui/
│   ├── browser/        # 文件浏览器界面
│   ├── player/         # 视频播放器界面
│   ├── viewer/         # 图片查看器界面
│   ├── settings/       # 设置界面
│   └── theme/          # Material3 主题
├── viewmodel/          # 带 StateFlow 的 ViewModels
├── navigation/         # NavGraph、界面路由
├── MainActivity.kt
└── WebDavApplication.kt
```

### 核心技术

- **WebDAV**：自定义 OkHttp 客户端，支持 PROPFIND/XML 解析
- **DI**：Hilt 依赖注入，使用 `@HiltViewModel`、`@AndroidEntryPoint`
- **UI**：Jetpack Compose + Material3 + Navigation Compose
- **媒体**：ExoPlayer (Media3) 播放视频，Coil 加载图片
- **状态**：StateFlow + sealed class 管理 UI 状态
- **异步**：Kotlin Coroutines

## WebDAV 实现

`data/remote/` 中的 `WebDAVClient` 处理：
- PROPFIND 请求和 XML 解析，用于目录列表
- 通过 OkHttp credentials 实现 HTTP Basic Auth
- 资源类型检测（VIDEO/IMAGE/AUDIO/OTHER）
- 特殊字符的自定义 URL 编码

### 错误处理

使用 `WebDAVException` sealed class：`ConnectionFailed`、`AuthenticationFailed`、`ResourceNotFound`、`InvalidResponse`。
Repository 方法返回 `Result<T>` 包装器。

## 开发指南

添加新功能时：
1. 在 `data/model/` 中创建数据模型
2. 在 `data/repository/` 中添加 repository 接口
3. 在 `WebDAVClient` 或 `ConfigDataStore` 中实现
4. 创建带 `StateFlow` 状态的 `@HiltViewModel`
5. 构建 Compose UI 界面
6. 在 `di/` 模块中注册新依赖

导航使用 `navigation/NavGraph.kt` 中定义的基于路由的导航。

## 额外说明

- 每次修改代码后需要解决编译错误和编译警告（编译命令 `./gradlew assembleDebug`）

## 相关文档

- [CODEBUDDY.md](CODEBUDDY.md) - 详细的开发指南
- [android_webdav_dev_steps.md](android_webdav_dev_steps.md) - 分步实现计划
- [android_webdav_viewer_design.md](android_webdav_viewer_design.md) - 技术架构决策