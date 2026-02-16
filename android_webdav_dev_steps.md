# Android WebDAV资源浏览器 - 开发步骤清单

> 本文档按AI开发工具可执行的方式拆解开发步骤，每个步骤包含：任务描述、输入依赖、输出产物、验收标准。

---

## 阶段一：项目初始化

### Step 1.1：创建Android项目
```
任务：创建基础Android项目结构
命令：使用Android Studio或命令行创建项目
配置：
  - 项目名：WebDAVViewer
  - 包名：com.tdull.webdavviewer
  - 语言：Kotlin
  - 最低SDK：26 (Android 8.0)
  - 构建配置：Kotlin DSL

输出产物：
  - app/build.gradle.kts
  - settings.gradle.kts
  - build.gradle.kts (root)
  - AndroidManifest.xml

验收标准：
  ✓ 项目可编译通过
  ✓ 可在模拟器/真机安装运行
```

### Step 1.2：配置项目依赖
```
任务：在app/build.gradle.kts中添加所有必需依赖

需要添加的依赖：
  // Compose BOM
  implementation(platform("androidx.compose:compose-bom:2024.02.00"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.activity:activity-compose:1.8.2")
  
  // Navigation
  implementation("androidx.navigation:navigation-compose:2.7.6")
  
  // Lifecycle
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
  
  // OkHttp
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  
  // ExoPlayer (Media3)
  implementation("androidx.media3:media3-exoplayer:1.2.1")
  implementation("androidx.media3:media3-ui:1.2.1")
  
  // Coil
  implementation("io.coil-kt:coil-compose:2.5.0")
  
  // Hilt
  implementation("com.google.dagger:hilt-android:2.50")
  kapt("com.google.dagger:hilt-compiler:2.50")
  implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
  
  // DataStore
  implementation("androidx.datastore:datastore-preferences:1.0.0")
  
  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

插件配置：
  id("com.google.dagger.hilt.android") version "2.50"
  id("com.google.devtools.ksp") version "1.9.22-1.0.17"

输出产物：
  - 更新后的app/build.gradle.kts

验收标准：
  ✓ Gradle Sync成功
  ✓ 所有依赖下载完成
```

### Step 1.3：配置Hilt依赖注入
```
任务：配置Hilt Application类和入口点

创建文件：
1. WebDavApplication.kt
   - 继承Application
   - 添加@HiltAndroidApp注解

2. 更新AndroidManifest.xml
   - 添加android:name=".WebDavApplication"
   - 添加INTERNET权限

输出产物：
  - app/src/main/java/com/example/webdavviewer/WebDavApplication.kt
  - 更新后的AndroidManifest.xml

验收标准：
  ✓ 项目编译通过
  ✓ 运行时Application初始化成功
```

### Step 1.4：创建基础目录结构
```
任务：创建项目的包目录结构

创建目录：
  app/src/main/java/com/example/webdavviewer/
  ├── data/
  │   ├── model/           # 数据模型
  │   ├── remote/          # 网络请求
  │   ├── local/           # 本地存储
  │   └── repository/      # 数据仓库
  ├── ui/
  │   ├── browser/         # 文件浏览器
  │   ├── player/          # 视频播放器
  │   ├── viewer/          # 图片查看器
  │   ├── settings/        # 设置页面
  │   └── theme/           # 主题样式
  ├── viewmodel/           # ViewModel
  ├── di/                  # 依赖注入模块
  └── navigation/          # 导航管理

输出产物：
  - 创建所有目录
  - 每个目录下添加.gitkeep文件

验收标准：
  ✓ 目录结构创建完成
```

---

## 阶段二：WebDAV核心模块

### Step 2.1：创建数据模型
```
任务：创建WebDAV相关的数据模型类

创建文件：
1. data/model/WebDAVResource.kt
   ```kotlin
   enum class ResourceType { DIRECTORY, VIDEO, IMAGE, AUDIO, OTHER }
   
   data class WebDAVResource(
       val path: String,
       val name: String,
       val isDirectory: Boolean,
       val size: Long = 0,
       val lastModified: Long = 0,
       val contentType: String? = null,
       val resourceType: ResourceType = ResourceType.OTHER
   ) {
       val isVideo: Boolean get() = resourceType == ResourceType.VIDEO
       val isImage: Boolean get() = resourceType == ResourceType.IMAGE
   }
   ```

2. data/model/ServerConfig.kt
   ```kotlin
   data class ServerConfig(
       val id: String = UUID.randomUUID().toString(),
       val name: String,
       val url: String,
       val username: String = "",
       val password: String = ""
   ) {
       init {
           require(url.isNotBlank()) { "URL不能为空" }
       }
   }
   ```

3. data/model/WebDAVException.kt
   ```kotlin
   sealed class WebDAVException : Exception() {
       class ConnectionFailed(cause: Throwable) : WebDAVException()
       class AuthenticationFailed : WebDAVException()
       class ResourceNotFound(path: String) : WebDAVException()
       class InvalidResponse(message: String) : WebDAVException()
   }
   ```

输出产物：
  - WebDAVResource.kt
  - ServerConfig.kt
  - WebDAVException.kt

验收标准：
  ✓ 所有类编译通过
  ✓ 数据类可正常实例化
```

### Step 2.2：实现WebDAV客户端
```
任务：实现WebDAVClient类，支持两种目录获取模式

创建文件：data/remote/WebDAVClient.kt

实现要点：
1. 使用OkHttp发送HTTP请求
2. 支持Basic认证
3. 支持两种服务器类型：
   - PROPFIND：标准WebDAV，发送PROPFIND请求获取目录内容
   - AUTOINDEX：Nginx autoindex，解析HTML目录列表
4. 自动检测服务器类型（可手动指定）
5. 解析XML响应（PROPFIND模式）或HTML目录列表（AUTOINDEX模式）
6. 处理HTTPS（信任自签名证书可选）

服务器类型枚举：
  enum class ServerType {
      PROPFIND,    // 标准WebDAV，支持PROPFIND请求
      AUTOINDEX    // Nginx autoindex，需解析HTML目录列表
  }

核心方法：
  - connect(config: ServerConfig): Boolean
  - listFiles(path: String): List<WebDAVResource>
  - getStreamUrl(path: String): String
  - testConnection(): Boolean
  - setServerType(type: ServerType): void  // 手动指定服务器类型
  - getServerType(): ServerType?           // 获取检测到的服务器类型

PROPFIND模式解析要点：
  - 响应格式：<?xml version="1.0"?><D:multistatus>...
  - 提取：<D:href>, <D:displayname>, <D:getcontentlength>, 
         <D:getlastmodified>, <D:resourcetype>
  - 使用Android内置XmlPullParser

AUTOINDEX模式解析要点：
  - Nginx autoindex HTML格式示例：
    <a href="file.mp4">file.mp4</a>  123456  16-Feb-2026 10:00
    <a href="dir/">dir/</a>           -       16-Feb-2026 10:00
  - 使用正则表达式提取：<a href="...">...</a>\s+size\s+date
  - 解析文件大小（支持K/M/G单位）
  - 解析日期格式（dd-MMM-yyyy HH:mm等）

自动检测逻辑：
  1. 首先尝试PROPFIND请求
  2. 如果返回207 Multi-Status → 使用PROPFIND模式
  3. 如果返回405/400/403 → 尝试GET请求
  4. 如果GET返回HTML目录列表 → 使用AUTOINDEX模式

输出产物：
  - WebDAVClient.kt

验收标准：
  ✓ 类编译通过
  ✓ PROPFIND请求格式正确
  ✓ XML解析逻辑完整
  ✓ HTML目录列表解析正确
  ✓ 自动检测服务器类型
```

### Step 2.3：创建WebDAV Repository
```
任务：创建数据仓库层，封装WebDAV操作

创建文件：data/repository/WebDAVRepository.kt

实现要点：
  - 注入WebDAVClient
  - 提供协程友好的API
  - 处理异常转换
  - 可选：添加内存缓存

接口定义：
  ```kotlin
  interface WebDAVRepository {
      suspend fun connect(config: ServerConfig): Result<Unit>
      suspend fun listFiles(path: String): Result<List<WebDAVResource>>
      fun getStreamUrl(path: String): String
      suspend fun testConnection(config: ServerConfig): Result<Boolean>
  }
  ```

实现类：
  ```kotlin
  class WebDAVRepositoryImpl @Inject constructor(
      private val client: WebDAVClient
  ) : WebDAVRepository { ... }
  ```

输出产物：
  - WebDAVRepository.kt (接口)
  - WebDAVRepositoryImpl.kt (实现)

验收标准：
  ✓ Repository编译通过
  ✓ 异常正确转换为Result
```

### Step 2.4：配置Hilt模块
```
任务：配置WebDAV模块的依赖注入

创建文件：di/WebDAVModule.kt

实现要点：
  ```kotlin
  @Module
  @InstallIn(SingletonComponent::class)
  object WebDAVModule {
      
      @Provides
      @Singleton
      fun provideOkHttpClient(): OkHttpClient {
          return OkHttpClient.Builder()
              .connectTimeout(30, TimeUnit.SECONDS)
              .readTimeout(30, TimeUnit.SECONDS)
              .build()
      }
      
      @Provides
      @Singleton
      fun provideWebDAVClient(okHttpClient: OkHttpClient): WebDAVClient {
          return WebDAVClient(okHttpClient)
      }
      
      @Binds
      @Singleton
      fun bindWebDAVRepository(
          impl: WebDAVRepositoryImpl
      ): WebDAVRepository
  }
  ```

输出产物：
  - WebDAVModule.kt

验收标准：
  ✓ Hilt模块编译通过
  ✓ 依赖注入配置正确
```

---

## 阶段三：配置管理模块

### Step 3.1：实现配置存储
```
任务：使用DataStore存储服务器配置

创建文件：
1. data/local/ConfigDataStore.kt
   ```kotlin
   class ConfigDataStore @Inject constructor(
       private val context: Context
   ) {
       private val Context.dataStore by preferencesDataStore("config")
       
       suspend fun saveServerConfig(config: ServerConfig)
       suspend fun getServerConfigs(): List<ServerConfig>
       suspend fun getActiveServer(): ServerConfig?
       suspend fun setActiveServer(id: String)
       suspend fun deleteServerConfig(id: String)
   }
   ```

2. data/repository/ConfigRepository.kt
   ```kotlin
   class ConfigRepository @Inject constructor(
       private val dataStore: ConfigDataStore
   ) {
       val servers: Flow<List<ServerConfig>>
       val activeServer: Flow<ServerConfig?>
       
       suspend fun addServer(config: ServerConfig)
       suspend fun removeServer(id: String)
       suspend fun setActiveServer(id: String)
   }
   ```

输出产物：
  - ConfigDataStore.kt
  - ConfigRepository.kt

验收标准：
  ✓ 配置可正常保存和读取
  ✓ Flow可正常发射数据
```

### Step 3.2：创建设置页面UI
```
任务：创建服务器配置管理界面

创建文件：
1. ui/settings/SettingsScreen.kt
   - 显示已保存的服务器列表
   - 添加新服务器按钮
   - 编辑/删除服务器选项

2. ui/settings/AddServerDialog.kt
   - 服务器名称输入
   - URL输入（验证格式）
   - 用户名/密码输入
   - 测试连接按钮
   - 保存/取消按钮

3. viewmodel/SettingsViewModel.kt
   - 管理服务器列表状态
   - 处理添加/删除/测试连接操作

输出产物：
  - SettingsScreen.kt
  - AddServerDialog.kt
  - SettingsViewModel.kt

验收标准：
  ✓ UI正常显示
  ✓ 可添加/删除服务器配置
  ✓ 测试连接功能正常
```

---

## 阶段四：文件浏览器模块

### Step 4.1：创建文件列表UI组件
```
任务：创建文件列表展示组件

创建文件：
1. ui/browser/FileBrowserScreen.kt
   ```kotlin
   @Composable
   fun FileBrowserScreen(
       viewModel: FileBrowserViewModel = hiltViewModel(),
       onVideoClick: (String) -> Unit,
       onImageClick: (String) -> Unit
   )
   ```

UI元素：
   - TopAppBar：显示当前路径/面包屑
   - LazyColumn：文件列表
   - FloatingActionButton：刷新按钮
   - 空状态提示
   - 加载状态（CircularProgressIndicator）
   - 错误状态（错误信息+重试按钮）

2. ui/browser/FileItem.kt
   ```kotlin
   @Composable
   fun FileItem(
       resource: WebDAVResource,
       onClick: () -> Unit
   )
   ```
   - 文件夹图标：黄色文件夹
   - 视频图标：视频播放图标
   - 图片图标：图片图标
   - 显示文件名、大小、修改时间

输出产物：
  - FileBrowserScreen.kt
  - FileItem.kt

验收标准：
  ✓ 列表UI正常渲染
  ✓ 不同类型文件显示不同图标
```

### Step 4.2：实现FileBrowserViewModel
```
任务：实现文件浏览器的ViewModel

创建文件：viewmodel/FileBrowserViewModel.kt

实现要点：
  ```kotlin
  @HiltViewModel
  class FileBrowserViewModel @Inject constructor(
      private val webDavRepository: WebDAVRepository,
      private val configRepository: ConfigRepository
  ) : ViewModel() {
      
      private val _uiState = MutableStateFlow<FileBrowserUiState>(...)
      val uiState: StateFlow<FileBrowserUiState> = _uiState
      
      private val _currentPath = MutableStateFlow("/")
      val currentPath: StateFlow<String> = _currentPath
      
      fun navigateTo(path: String)
      fun navigateUp()
      fun refresh()
      fun selectServer(config: ServerConfig)
  }
  
  data class FileBrowserUiState(
      val isLoading: Boolean = false,
      val files: List<WebDAVResource> = emptyList(),
      val error: String? = null,
      val isConnected: Boolean = false
  )
  ```

输出产物：
  - FileBrowserViewModel.kt
  - FileBrowserUiState.kt

验收标准：
  ✓ ViewModel编译通过
  ✓ StateFlow正确更新
```

### Step 4.3：实现面包屑导航
```
任务：实现路径导航面包屑组件

创建文件：ui/browser/Breadcrumb.kt

实现要点：
  ```kotlin
  @Composable
  fun Breadcrumb(
      path: String,
      onNavigate: (String) -> Unit
  )
  ```
   - 将路径拆分为层级：/ -> /Videos/ -> /Videos/Movies/
   - 每个层级可点击跳转
   - 支持水平滚动（长路径）
   - 当前层级高亮显示

输出产物：
  - Breadcrumb.kt

验收标准：
  ✓ 面包屑正确显示路径层级
  ✓ 点击可跳转到对应目录
```

---

## 阶段五：视频播放模块

### Step 5.1：创建视频播放器UI
```
任务：创建ExoPlayer视频播放界面

创建文件：
1. ui/player/VideoPlayerScreen.kt
   ```kotlin
   @Composable
   fun VideoPlayerScreen(
       videoUrl: String,
       viewModel: VideoPlayerViewModel = hiltViewModel()
   )
   ```
   UI元素：
   - ExoPlayer视图（使用androidx.media3.ui.PlayerView）
   - 播放控制栏（播放/暂停/进度条/全屏）
   - 返回按钮
   - 视频标题

2. ui/theme/PlayerTheme.kt
   - 视频播放器专用深色主题

输出产物：
  - VideoPlayerScreen.kt
  - PlayerTheme.kt

验收标准：
  ✓ 播放器UI正常显示
  ✓ 可播放网络视频流
```

### Step 5.2：实现VideoPlayerViewModel
```
任务：实现视频播放器的ViewModel

创建文件：viewmodel/VideoPlayerViewModel.kt

实现要点：
  ```kotlin
  @HiltViewModel
  class VideoPlayerViewModel @Inject constructor(
      private val savedStateHandle: SavedStateHandle
  ) : ViewModel() {
      
      private val _player = MutableStateFlow<ExoPlayer?>(null)
      val player: StateFlow<ExoPlayer?> = _player
      
      fun initializePlayer(url: String)
      fun releasePlayer()
      
      override fun onCleared() {
          super.onCleared()
          releasePlayer()
      }
  }
  ```

输出产物：
  - VideoPlayerViewModel.kt

验收标准：
  ✓ ExoPlayer正确初始化
  ✓ 生命周期正确管理
```

### Step 5.3：配置ExoPlayer依赖注入
```
任务：创建ExoPlayer的Hilt模块

创建文件：di/PlayerModule.kt

实现要点：
  ```kotlin
  @Module
  @InstallIn(ActivityComponent::class)
  object PlayerModule {
      
      @Provides
      fun provideExoPlayer(
          context: Context,
          webDavClient: WebDAVClient
      ): ExoPlayer {
          return ExoPlayer.Builder(context)
              .build()
      }
  }
  ```

输出产物：
  - PlayerModule.kt

验收标准：
  ✓ ExoPlayer可正常注入
```

---

## 阶段六：图片查看模块

### Step 6.1：创建图片查看器UI
```
任务：创建图片查看界面

创建文件：ui/viewer/ImageViewerScreen.kt

实现要点：
  ```kotlin
  @Composable
  fun ImageViewerScreen(
      imageUrl: String,
      onDismiss: () -> Unit
  )
  ```
   - 使用Coil加载图片
   - 支持缩放手势（pinch-to-zoom）
   - 支持双击放大/缩小
   - 支持拖动手势
   - 全屏显示
   - 点击空白区域关闭
   - 加载中占位图
   - 加载失败提示

依赖：
  - 使用 Accompanist 或 Modifier.transformable 处理手势

输出产物：
  - ImageViewerScreen.kt

验收标准：
  ✓ 图片正常加载显示
  ✓ 缩放手势正常工作
```

### Step 6.2：实现图片手势处理
```
任务：实现图片缩放和拖动手势

创建文件：ui/viewer/ZoomableImage.kt

实现要点：
  ```kotlin
  @Composable
  fun ZoomableImage(
      imageUrl: String,
      modifier: Modifier = Modifier
  )
  ```
   - 使用rememberTransformableState管理变换状态
   - 限制缩放范围（0.5x - 5x）
   - 边界检测
   - 双击动画缩放

输出产物：
  - ZoomableImage.kt

验收标准：
  ✓ 手势流畅响应
  ✓ 变换范围合理
```

---

## 阶段七：导航集成

### Step 7.1：创建导航图
```
任务：创建应用导航结构

创建文件：navigation/Screen.kt

实现要点：
  ```kotlin
  sealed class Screen(val route: String) {
      object Settings : Screen("settings")
      object Browser : Screen("browser?serverId={serverId}")
      object VideoPlayer : Screen("video?url={url}")
      object ImageViewer : Screen("image?url={url}")
  }
  ```

创建文件：navigation/NavGraph.kt

实现要点：
  ```kotlin
  @Composable
  fun AppNavGraph(
      navController: NavHostController,
      modifier: Modifier = Modifier
  ) {
      NavHost(
          navController = navController,
          startDestination = Screen.Settings.route,
          modifier = modifier
      ) {
          composable(Screen.Settings.route) { ... }
          composable(Screen.Browser.route) { ... }
          composable(Screen.VideoPlayer.route) { ... }
          composable(Screen.ImageViewer.route) { ... }
      }
  }
  ```

输出产物：
  - Screen.kt
  - NavGraph.kt

验收标准：
  ✓ 导航图定义完整
  ✓ 路由参数传递正确
```

### Step 7.2：创建MainActivity
```
任务：创建主Activity并集成导航

创建文件：MainActivity.kt

实现要点：
  ```kotlin
  @AndroidEntryPoint
  class MainActivity : ComponentActivity() {
      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          setContent {
              WebDAVViewerTheme {
                  val navController = rememberNavController()
                  AppNavGraph(navController = navController)
              }
          }
      }
  }
  ```

输出产物：
  - MainActivity.kt

验收标准：
  ✓ 应用启动正常
  ✓ 导航切换正常
```

---

## 阶段八：主题和样式

### Step 8.1：创建应用主题
```
任务：创建Material3主题配置

创建文件：
1. ui/theme/Color.kt
   - 定义主色调
   - 定义次色调
   - 定义背景色

2. ui/theme/Theme.kt
   ```kotlin
   @Composable
   fun WebDAVViewerTheme(
       darkTheme: Boolean = isSystemInDarkTheme(),
       content: @Composable () -> Unit
   )
   ```

3. ui/theme/Type.kt
   - 定义Typography配置

输出产物：
  - Color.kt
  - Theme.kt
  - Type.kt

验收标准：
  ✓ 主题正常应用
  ✓ 支持深色/浅色模式
```

### Step 8.2：添加应用图标
```
任务：配置应用图标

文件位置：
  - app/src/main/res/mipmap-*/ic_launcher.png
  - app/src/main/res/mipmap-*/ic_launcher_round.png

设计要求：
  - 简洁的文件夹/云存储图标
  - 符合Material Design规范

输出产物：
  - 各分辨率的图标文件

验收标准：
  ✓ 图标在启动器正确显示
```

---

## 阶段九：错误处理和优化

### Step 9.1：完善错误处理
```
任务：添加全面的错误处理机制

实现要点：
1. 网络错误
   - 无网络连接提示
   - 连接超时处理
   - 服务器无响应处理

2. 认证错误
   - 凭据无效提示
   - 重新输入凭据引导

3. 资源错误
   - 文件不存在提示
   - 不支持的格式提示

4. 全局异常处理
   - 未捕获异常记录
   - 友好的崩溃提示

输出产物：
  - 更新各ViewModel的错误处理
  - 添加通用错误UI组件

验收标准：
  ✓ 各种错误场景有友好提示
  ✓ 应用不会意外崩溃
```

### Step 9.2：性能优化
```
任务：优化应用性能

优化项：
1. 图片加载优化
   - 使用Coil的内存缓存
   - 合理的图片缩放

2. 列表滚动优化
   - LazyColumn key优化
   - 合理的item复用

3. 网络请求优化
   - 目录列表缓存
   - 预加载下一页

4. 内存优化
   - 及时释放资源
   - 避免内存泄漏

输出产物：
  - 更新各组件的优化代码

验收标准：
  ✓ 列表滚动流畅
  ✓ 图片加载快速
  ✓ 内存占用合理
```

---

## 阶段十：测试和发布

### Step 10.1：编写单元测试
```
任务：编写关键模块的单元测试

测试范围：
1. WebDAVClient测试
   - PROPFIND请求格式验证
   - XML解析正确性验证

2. Repository测试
   - 数据转换正确性
   - 异常处理正确性

3. ViewModel测试
   - 状态变化正确性
   - 事件处理正确性

输出产物：
  - app/src/test/java/ 目录下的测试文件

验收标准：
  ✓ 测试覆盖率 > 60%
  ✓ 所有测试通过
```

### Step 10.2：编写UI测试
```
任务：编写关键流程的UI测试

测试范围：
1. 服务器配置流程
   - 添加服务器
   - 测试连接
   - 删除服务器

2. 文件浏览流程
   - 进入目录
   - 返回上级
   - 刷新列表

输出产物：
  - app/src/androidTest/java/ 目录下的测试文件

验收标准：
  ✓ 关键流程测试通过
```

### Step 10.3：生成发布版本
```
任务：生成签名的发布APK/AAB

步骤：
1. 配置签名密钥
   - 创建keystore文件
   - 配置build.gradle签名

2. 构建发布版本
   - ./gradlew assembleRelease
   - 或 ./gradlew bundleRelease

3. 混淆配置
   - 配置proguard-rules.pro
   - 保留必要类不被混淆

输出产物：
  - app/release/app-release.apk
  - 或 app/release/app-release.aab

验收标准：
  ✓ 发布包生成成功
  ✓ 安装后正常运行
```

---

## 开发顺序总结

```
阶段一：项目初始化
  Step 1.1 → Step 1.2 → Step 1.3 → Step 1.4

阶段二：WebDAV核心模块
  Step 2.1 → Step 2.2 → Step 2.3 → Step 2.4

阶段三：配置管理模块
  Step 3.1 → Step 3.2

阶段四：文件浏览器模块
  Step 4.1 → Step 4.2 → Step 4.3

阶段五：视频播放模块
  Step 5.1 → Step 5.2 → Step 5.3

阶段六：图片查看模块
  Step 6.1 → Step 6.2

阶段七：导航集成
  Step 7.1 → Step 7.2

阶段八：主题和样式
  Step 8.1 → Step 8.2

阶段九：错误处理和优化
  Step 9.1 → Step 9.2

阶段十：测试和发布
  Step 10.1 → Step 10.2 → Step 10.3
```

---

## AI执行提示

每完成一个Step，AI工具应：

1. ✅ 确认输出产物已创建
2. ✅ 执行验收标准检查
3. ✅ 解决编译错误（如有）
4. ✅ 更新开发进度记录

如遇问题：
- 标注阻塞原因
- 提供可能的解决方案
- 等待用户决策
