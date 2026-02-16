# Android WebDAV资源浏览器 - 技术方案

## 一、技术栈选择

### 推荐方案：原生Android + Kotlin

**理由：**
- AI工具对Kotlin/Android SDK的代码生成能力强
- 现成库支持丰富，减少底层实现
- 文档完善，AI容易理解上下文

**核心依赖库：**

| 功能模块 | 推荐库 | 说明 |
|---------|--------|------|
| WebDAV协议 | `okhttp` + 手动解析 | 支持PROPFIND和Nginx autoindex两种模式 |
| 视频播放 | `ExoPlayer` (现Media3) | Google官方，支持mp4/mkv |
| 图片加载 | `Coil` | Kotlin协程友好 |
| UI框架 | `Jetpack Compose` | 现代UI，AI生成效率高 |

---

## 二、架构设计

```
┌─────────────────────────────────────────┐
│              UI Layer (Compose)          │
│  ┌──────────┐  ┌──────────┐  ┌────────┐│
│  │文件浏览器│  │视频播放器│  │图片查看││
│  └──────────┘  └──────────┘  └────────┘│
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│           ViewModel Layer                │
│  FileListViewModel  PlayerViewModel     │
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│           Repository Layer               │
│  WebDAVRepository  (统一数据访问)         │
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│           Data Layer                     │
│  WebDAVClient  │  CacheManager           │
└─────────────────────────────────────────┘
```

---

## 三、模块拆分（AI开发友好）

### 模块1：WebDAV客户端核心
```
功能：
- 连接配置（服务器地址、用户名、密码）
- 支持两种服务器类型：
  1. 标准WebDAV (PROPFIND请求)
  2. Nginx autoindex (HTML目录列表解析)
- 自动检测服务器类型（可手动指定）
- 资源类型识别（文件/文件夹/类型）
- 下载/流式URL生成

服务器类型枚举：
  enum class ServerType {
      PROPFIND,    // 标准WebDAV
      AUTOINDEX    // Nginx autoindex
  }

输出：
- WebDAVClient.kt (核心类)
- ServerType.kt (服务器类型枚举)
- WebDAVResource.kt (数据模型)
- WebDAVException.kt (异常处理)
```

### 模块2：文件浏览器
```
功能：
- 面包屑导航
- 文件列表展示（图标区分类型）
- 点击文件夹进入下级
- 长按/右键菜单操作

输出：
- FileBrowserScreen.kt
- FileListViewModel.kt
- FileItem.kt
```

### 模块3：视频播放器
```
功能：
- ExoPlayer集成
- 播放控制（播放/暂停/进度条）
- 支持HTTP/WebDAV流式播放
- 全屏切换

输出：
- VideoPlayerScreen.kt
- VideoPlayerViewModel.kt
```

### 模块4：图片查看器
```
功能：
- Coil图片加载
- 缩放手势
- 左右滑动切换（同目录图片）

输出：
- ImageViewerScreen.kt
- ImageViewerViewModel.kt
```

### 模块5：配置管理
```
功能：
- 服务器连接配置保存
- 账号管理

输出：
- SettingsScreen.kt
- ServerConfig.kt (数据类)
- ConfigRepository.kt (SharedPreferences/DataStore)
```

---

## 四、数据模型定义

```kotlin
// WebDAV资源模型
data class WebDAVResource(
    val path: String,           // 完整路径
    val name: String,           // 文件名
    val isDirectory: Boolean,   // 是否文件夹
    val size: Long,             // 文件大小
    val lastModified: Long,     // 修改时间
    val contentType: String?,   // MIME类型
    val resourceType: ResourceType // 枚举：VIDEO/IMAGE/OTHER
)

// 服务器配置
data class ServerConfig(
    val id: String,
    val name: String,
    val url: String,            // WebDAV地址
    val username: String,
    val password: String
)
```

---

## 五、AI开发执行计划

### 阶段1：项目初始化（1次交互）
- 创建Android项目（Kotlin + Compose）
- 配置build.gradle依赖
- 设置基础架构（Hilt依赖注入、Navigation）

### 阶段2：WebDAV核心模块（2-3次交互）
- 实现WebDAVClient（OkHttp发送PROPFIND请求）
- 解析XML响应
- 单元测试验证

### 阶段3：文件浏览器（2次交互）
- 文件列表UI
- 导航逻辑
- 与WebDAVClient集成

### 阶段4：媒体播放（2次交互）
- ExoPlayer集成
- 视频播放界面
- 图片查看界面

### 阶段5：配置与完善（1-2次交互）
- 服务器配置界面
- 数据持久化
- 错误处理优化

---

## 六、关键技术点

### WebDAV客户端双模式支持

WebDAVClient支持两种目录获取模式，自动检测服务器类型：

#### 模式1：标准WebDAV PROPFIND请求
```kotlin
// PROPFIND请求（标准WebDAV）
suspend fun listFilesViaPropfind(path: String): List<WebDAVResource> {
    val request = Request.Builder()
        .url("$baseUrl$path")
        .method("PROPFIND", 
            """<?xml version="1.0"?>
               <propfind xmlns="DAV:">
                 <prop><displayname/><getcontentlength/>
                       <getlastmodified/><resourcetype/></prop>
               </propfind>""".toRequestBody())
        .header("Depth", "1")
        .build()
    // 解析XML multistatus响应...
}
```

#### 模式2：Nginx Autoindex HTML解析
```kotlin
// GET请求获取HTML目录列表（Nginx autoindex）
suspend fun listFilesViaAutoindex(path: String): List<WebDAVResource> {
    val request = Request.Builder()
        .url("$baseUrl$path/")
        .build()
    
    val html = response.body?.string() ?: ""
    // 使用正则解析HTML：
    // <a href="file.mp4">file.mp4</a>  123456  16-Feb-2026 10:00
    val pattern = Pattern.compile(
        """<a\s+href="([^"]+)"[^>]*>([^<]+)</a>\s+(\S+)\s+(\S+\s+\S+)"""
    )
    // 提取: href, name, size, date
}
```

#### 自动检测逻辑
```kotlin
fun detectServerType(config: ServerConfig): ServerType {
    // 1. 尝试PROPFIND请求
    // 2. 如果返回207 Multi-Status → PROPFIND模式
    // 3. 如果返回405/400 → 尝试GET请求
    // 4. 如果GET返回HTML目录列表 → AUTOINDEX模式
}
```

### ExoPlayer流式播放
```kotlin
val mediaItem = MediaItem.fromUri(webDavClient.getStreamUrl(path))
exoPlayer.setMediaItem(mediaItem)
exoPlayer.prepare()
```

---

## 七、预期产出物

```
app/
├── src/main/java/com/example/webdavviewer/
│   ├── data/
│   │   ├── remote/WebDAVClient.kt
│   │   ├── model/WebDAVResource.kt
│   │   └── repository/WebDAVRepository.kt
│   ├── ui/
│   │   ├── browser/FileBrowserScreen.kt
│   │   ├── player/VideoPlayerScreen.kt
│   │   ├── viewer/ImageViewerScreen.kt
│   │   └── settings/SettingsScreen.kt
│   ├── viewmodel/
│   │   ├── FileBrowserViewModel.kt
│   │   └── PlayerViewModel.kt
│   └── di/AppModule.kt
└── build.gradle.kts
```

---

## 八、依赖配置参考

### build.gradle.kts (Module: app)

```kotlin
dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // OkHttp (WebDAV)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
    
    // ExoPlayer (Media3)
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    
    // Coil (Image Loading)
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Hilt (DI)
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

---

## 九、开发注意事项

1. **WebDAV认证**：需要处理Basic Auth和Digest Auth两种认证方式
2. **HTTPS证书**：支持自签名证书的场景
3. **大文件处理**：视频使用流式播放，避免完整下载
4. **缓存策略**：目录列表可适当缓存，减少请求
5. **错误处理**：网络异常、认证失败、文件不存在等场景
6. **服务器兼容性**：
   - 标准WebDAV服务器使用PROPFIND请求
   - Nginx内置WebDAV模块不支持PROPFIND，需解析autoindex HTML
   - 自动检测服务器类型，也可手动指定

---

## 十、扩展功能（可选）

- [ ] 多服务器管理
- [ ] 离线下载
- [ ] 文件搜索
- [ ] 文件排序（按名称/大小/时间）
- [ ] 视频字幕支持
- [ ] 图片幻灯片播放
- [ ] 文件分享链接生成
