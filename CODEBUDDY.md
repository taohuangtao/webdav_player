# CODEBUDDY.md

本文件为 CodeBuddy 在此代码库中工作时提供指导。

## 项目概述

WebDAVViewer 是一个用于浏览和查看 WebDAV 资源（文件、视频、图片）的 Android 应用程序。使用 Kotlin、Jetpack Compose 和 Material3 构建。

## 构建命令

```bash
# 构建项目
./gradlew assembleDebug

# 构建 release APK
./gradlew assembleRelease

# 清理构建
./gradlew clean

# 运行单元测试
gradlew test

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
┌─────────────────────────────────────┐
│  UI 层（Jetpack Compose）             │
│  - browser/, player/, viewer/,       │
│    settings/, theme/                 │
└─────────────────────────────────────┘
                ↓
┌─────────────────────────────────────┐
│  ViewModel 层                        │
│  - StateFlow 管理 UI 状态             │
│  - Hilt 依赖注入                     │
└─────────────────────────────────────┘
                ↓
┌─────────────────────────────────────┐
│  Repository 层                       │
│  - WebDAVRepository                  │
│  - ConfigRepository                  │
└─────────────────────────────────────┘
                ↓
┌─────────────────────────────────────┐
│  Data 层                             │
│  - WebDAVClient (remote/)            │
│  - ConfigDataStore (local/)          │
│  - Models (data/model/)              │
└─────────────────────────────────────┘
```

### 包结构

```
com.tdull.webdavviewer.app/
├── data/
│   ├── model/          # 数据类：WebDAVResource, ServerConfig, WebDAVException
│   ├── remote/         # WebDAVClient（基于 OkHttp 的 PROPFIND 请求）
│   ├── local/          # ConfigDataStore（SharedPreferences/DataStore）
│   └── repository/     # WebDAVRepository 接口和实现
├── di/                 # Hilt 模块：WebDAVModule
├── ui/                 # Compose 界面：browser/, player/, viewer/, settings/, theme/
├── viewmodel/          # 各界面的 ViewModels
├── navigation/         # 导航图和界面定义
├── MainActivity.kt     # 单 Activity，带 @AndroidEntryPoint
└── WebDavApplication.kt # Hilt Application 类
```

### 核心技术

- **WebDAV 协议**：自定义 OkHttp 客户端，发送 PROPFIND 请求并解析 XML
- **依赖注入**：Hilt，使用 `@HiltAndroidApp`、`@AndroidEntryPoint`、`@HiltViewModel`
- **UI**：Jetpack Compose + Material3 + Navigation Compose
- **媒体**：ExoPlayer (Media3) 播放视频，Coil 加载图片
- **状态管理**：StateFlow + ViewModel，sealed class 管理 UI 状态
- **异步**：Kotlin Coroutines，suspend 函数

## WebDAV 实现

`WebDAVClient` 类处理所有 WebDAV 操作：

1. **PROPFIND 请求**：发送 XML 请求体获取目录内容
2. **XML 解析**：使用 Android 的 XmlPullParser 解析 multistatus 响应
3. **身份验证**：通过 OkHttp credentials 实现 HTTP Basic Auth
4. **URL 编码**：自定义路径编码，处理文件名中的特殊字符
5. **资源类型检测**：基于 MIME 类型和扩展名的分类（VIDEO/IMAGE/AUDIO/OTHER）

### WebDAVResource 模型

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

## 开发指南

### 添加新功能时

1. **数据层优先**：先在 `data/model/` 中创建模型，然后添加 repository 方法
2. **Repository 模式**：所有数据访问通过 repository 接口
3. **依赖注入**：在 `di/` 模块中注册新依赖
4. **ViewModel**：使用 `@HiltViewModel` 并通过 `StateFlow` 暴露状态
5. **Compose UI**：保持界面无状态，通过回调处理导航

### 测试 WebDAV 功能

测试 WebDAV 连接时：
- 操作前使用 `testConnection(config: ServerConfig)`
- 处理 `WebDAVException` sealed class（ConnectionFailed, AuthenticationFailed, ResourceNotFound, InvalidResponse）
- 使用 Result 包装器模式：`Result<T>` 进行错误处理

### 媒体播放

- 视频：使用 `getStreamUrl(path)` 获取 ExoPlayer 的直接 HTTP URL
- 图片：通过 Coil 加载，需要时附带 WebDAV 身份验证头
- 直接流式传输，不要下载完整文件

## 依赖项

所有依赖在 `app/build.gradle.kts` 中管理：
- Compose BOM 2024.02.00
- Navigation Compose 2.7.6
- Hilt 2.50
- OkHttp 4.12.0
- Media3 ExoPlayer 1.2.1
- Coil 2.5.0
- DataStore 1.0.0
- Kotlin Coroutines 1.7.3

## Android Manifest

- `android:usesCleartextTraffic="true"` 已启用，支持 HTTP WebDAV 服务器
- INTERNET 和 ACCESS_NETWORK_STATE 权限
- 单 MainActivity，带启动 intent

## 额外说明

- 每次修改代码后需要解决编译错误和编译警告（编译命令 `./gradlew assembleDebug`）
- 注意当前的开发环境是windows11操作系统，在执行命令时需要采用相关支持的命令格式