package com.tdull.webdavviewer.app.viewmodel

import android.app.Application
import app.cash.turbine.test
import com.tdull.webdavviewer.app.data.model.DownloadItem
import com.tdull.webdavviewer.app.data.model.FavoriteItem
import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.data.model.WebDAVException
import com.tdull.webdavviewer.app.data.model.WebDAVResource
import com.tdull.webdavviewer.app.data.repository.ConfigRepository
import com.tdull.webdavviewer.app.data.repository.DownloadsRepository
import com.tdull.webdavviewer.app.data.repository.FavoritesRepository
import com.tdull.webdavviewer.app.data.repository.WebDAVRepository
import com.tdull.webdavviewer.app.service.DownloadManager
import com.tdull.webdavviewer.app.util.NetworkMonitor
import com.tdull.webdavviewer.app.util.NetworkStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * FileBrowserViewModel 单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FileBrowserViewModelTest {

    @Mock
    private lateinit var mockApplication: Application

    @Mock
    private lateinit var mockWebDavRepository: WebDAVRepository

    @Mock
    private lateinit var mockConfigRepository: ConfigRepository

    @Mock
    private lateinit var mockNetworkMonitor: NetworkMonitor

    @Mock
    private lateinit var mockFavoritesRepository: FavoritesRepository

    @Mock
    private lateinit var mockDownloadsRepository: DownloadsRepository

    @Mock
    private lateinit var mockDownloadManager: DownloadManager

    private lateinit var viewModel: FileBrowserViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // 默认配置
        whenever(mockConfigRepository.servers).thenReturn(flowOf(emptyList()))
        whenever(mockConfigRepository.activeServer).thenReturn(flowOf(null))
        whenever(mockNetworkMonitor.networkStatus).thenReturn(flowOf(NetworkStatus(isAvailable = true)))
        whenever(mockNetworkMonitor.isNetworkAvailable()).thenReturn(true)
        whenever(mockFavoritesRepository.favorites).thenReturn(flowOf(emptyList<FavoriteItem>()))
        whenever(mockDownloadsRepository.downloads).thenReturn(flowOf(emptyList<DownloadItem>()))
        whenever(mockDownloadManager.downloadProgress).thenReturn(MutableStateFlow(emptyMap()))

        // ErrorHandler 依赖 application.getString 获取文案，mock 默认返回 null 会导致 error 为 null 或抛 NPE
        whenever(mockApplication.getString(anyInt())).thenReturn("mock_title")
        whenever(mockApplication.getString(anyInt(), any())).thenReturn("mock_message")

        viewModel = FileBrowserViewModel(
            application = mockApplication,
            webDavRepository = mockWebDavRepository,
            configRepository = mockConfigRepository,
            networkMonitor = mockNetworkMonitor,
            favoritesRepository = mockFavoritesRepository,
            downloadsRepository = mockDownloadsRepository,
            downloadManager = mockDownloadManager
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // ========== 初始状态测试 ==========

    @Test
    fun `initial state is correct`() = runTest {
        val initialState = viewModel.uiState.value
        
        assertFalse(initialState.isLoading)
        assertTrue(initialState.files.isEmpty())
        assertNull(initialState.error)
        assertNull(initialState.errorInfo)
        assertFalse(initialState.isConnected)
        assertNull(initialState.currentServer)
        assertTrue(initialState.isNetworkAvailable)
    }

    @Test
    fun `initial path is root`() = runTest {
        assertEquals("/", viewModel.currentPath.value)
    }

    // ========== selectServer 测试 ==========

    @Test
    fun `selectServer updates currentServer and connects`() = runTest {
        val config = ServerConfig(
            id = "test-id",
            name = "Test Server",
            url = "https://example.com"
        )
        val files = listOf(
            WebDAVResource(path = "/folder", name = "folder", isDirectory = true),
            WebDAVResource(path = "/file.txt", name = "file.txt", isDirectory = false)
        )

        whenever(mockWebDavRepository.connect(config)).thenReturn(Result.success(Unit))
        whenever(mockWebDavRepository.listFiles("/", false)).thenReturn(Result.success(files))

        viewModel.selectServer(config)

        viewModel.uiState.test {
            val finalState = awaitItem()
            assertEquals(config, finalState.currentServer)
            assertTrue(finalState.isConnected)
            assertEquals(2, finalState.files.size)
            assertNull(finalState.error)
        }
    }

    @Test
    fun `selectServer handles connection failure`() = runTest {
        val config = ServerConfig(
            name = "Test Server",
            url = "https://example.com"
        )

        whenever(mockWebDavRepository.connect(config))
            .thenReturn(Result.failure(WebDAVException.AuthenticationFailed()))

        viewModel.selectServer(config)

        viewModel.uiState.test {
            val finalState = awaitItem()
            assertEquals(config, finalState.currentServer)
            assertFalse(finalState.isConnected)
            assertNotNull(finalState.error)
            assertNotNull(finalState.errorInfo)
        }
    }

    @Test
    fun `selectServer shows error when network unavailable`() = runTest {
        val config = ServerConfig(
            name = "Test Server",
            url = "https://example.com"
        )

        whenever(mockNetworkMonitor.isNetworkAvailable()).thenReturn(false)

        viewModel.selectServer(config)

        // isNetworkAvailable 由 networkStatus flow 驱动，selectServer 的网络检查仅同步判断并设置错误状态
        viewModel.uiState.test {
            val finalState = awaitItem()
            assertFalse(finalState.isConnected)
            assertNotNull(finalState.error)
            assertNotNull(finalState.errorInfo)
        }
    }

    // ========== navigateTo 测试 ==========

    @Test
    fun `navigateTo updates currentPath and loads files`() = runTest {
        val files = listOf(
            WebDAVResource(path = "/subfolder/file.txt", name = "file.txt", isDirectory = false)
        )
        // selectServer 成功后会加载根目录，navigateTo 会加载目标目录，均需 stub
        whenever(mockWebDavRepository.listFiles("/", false)).thenReturn(Result.success(emptyList()))
        whenever(mockWebDavRepository.listFiles("/subfolder", false)).thenReturn(Result.success(files))

        // 先连接服务器
        val config = ServerConfig(name = "Test", url = "https://example.com")
        whenever(mockWebDavRepository.connect(config)).thenReturn(Result.success(Unit))
        viewModel.selectServer(config)

        viewModel.navigateTo("/subfolder")

        assertEquals("/subfolder", viewModel.currentPath.value)
        verify(mockWebDavRepository).listFiles("/subfolder", false)
    }

    // ========== navigateUp 测试 ==========

    @Test
    fun `navigateUp returns to previous path`() = runTest {
        whenever(mockWebDavRepository.listFiles(any(), any<Boolean>())).thenReturn(Result.success(emptyList()))

        // 先连接服务器
        val config = ServerConfig(name = "Test", url = "https://example.com")
        viewModel.selectServer(config)

        // 导航到子目录
        viewModel.navigateTo("/subfolder")
        assertEquals("/subfolder", viewModel.currentPath.value)

        // 返回上级
        viewModel.navigateUp()
        assertEquals("/", viewModel.currentPath.value)
    }

    @Test
    fun `navigateUp stays at root when already at root`() = runTest {
        whenever(mockWebDavRepository.listFiles(any(), any<Boolean>())).thenReturn(Result.success(emptyList()))

        val config = ServerConfig(name = "Test", url = "https://example.com")
        viewModel.selectServer(config)

        viewModel.navigateUp()

        assertEquals("/", viewModel.currentPath.value)
    }

    // ========== refresh 测试 ==========

    @Test
    fun `refresh reloads current directory`() = runTest {
        whenever(mockWebDavRepository.listFiles("/", false)).thenReturn(Result.success(emptyList()))

        val config = ServerConfig(name = "Test", url = "https://example.com")
        whenever(mockWebDavRepository.connect(config)).thenReturn(Result.success(Unit))
        viewModel.selectServer(config)

        viewModel.refresh()

        // selectServer 成功后自动加载一次根目录，refresh 再加载一次，共 2 次
        verify(mockWebDavRepository, times(2)).listFiles("/", false)
    }

    // ========== getStreamUrl 测试 ==========

    @Test
    fun `getStreamUrl returns correct URL`() {
        val expectedUrl = "https://example.com/video.mp4"
        whenever(mockWebDavRepository.getStreamUrl("/video.mp4")).thenReturn(expectedUrl)

        val result = viewModel.getStreamUrl("/video.mp4")

        assertEquals(expectedUrl, result)
    }

    // ========== 隐藏文件显示测试 ==========

    /**
     * 构造包含隐藏文件与普通文件的目录列表
     */
    private fun buildFilesWithHidden(): List<WebDAVResource> {
        return listOf(
            WebDAVResource(path = "/folder", name = "folder", isDirectory = true),
            WebDAVResource(path = "/file.txt", name = "file.txt", isDirectory = false),
            WebDAVResource(path = "/.DS_Store", name = ".DS_Store", isDirectory = false),
            WebDAVResource(path = "/.hidden_dir", name = ".hidden_dir", isDirectory = true),
            WebDAVResource(path = "/.gitignore", name = ".gitignore", isDirectory = false)
        )
    }

    @Test
    fun `showHidden defaults to false`() {
        assertFalse(viewModel.uiState.value.showHidden)
    }

    @Test
    fun `loadFiles passes showHidden=false by default`() = runTest {
        val config = ServerConfig(name = "Test", url = "https://example.com")
        whenever(mockWebDavRepository.connect(config)).thenReturn(Result.success(Unit))
        // 数据层已按 showHidden=false 返回过滤后的普通文件列表
        val filteredFiles = listOf(
            WebDAVResource(path = "/folder", name = "folder", isDirectory = true),
            WebDAVResource(path = "/file.txt", name = "file.txt", isDirectory = false)
        )
        whenever(mockWebDavRepository.listFiles("/", false)).thenReturn(Result.success(filteredFiles))

        viewModel.selectServer(config)

        verify(mockWebDavRepository).listFiles("/", false)
        val names = viewModel.uiState.value.files.map { it.name }
        assertFalse("默认状态下不应包含隐藏文件: $names", names.count { it.startsWith(".") } > 0)
        assertTrue(names.contains("folder"))
        assertTrue(names.contains("file.txt"))
    }

    @Test
    fun `toggleShowHidden flips state and reloads current directory`() = runTest {
        val config = ServerConfig(name = "Test", url = "https://example.com")
        whenever(mockWebDavRepository.connect(config)).thenReturn(Result.success(Unit))
        whenever(mockWebDavRepository.listFiles("/", false)).thenReturn(Result.success(emptyList()))
        whenever(mockWebDavRepository.listFiles("/", true)).thenReturn(Result.success(emptyList()))

        viewModel.selectServer(config)
        assertFalse(viewModel.uiState.value.showHidden)

        viewModel.toggleShowHidden()
        assertTrue(viewModel.uiState.value.showHidden)
        verify(mockWebDavRepository).listFiles("/", true)

        viewModel.toggleShowHidden()
        assertFalse(viewModel.uiState.value.showHidden)
        // listFiles("/", false) 在 selectServer 连接后加载根目录时调用过 1 次，二次 toggle 后再调用 1 次，共 2 次
        verify(mockWebDavRepository, times(2)).listFiles("/", false)
    }

    @Test
    fun `loadFiles requests showHidden=true and exposes hidden files`() = runTest {
        val config = ServerConfig(name = "Test", url = "https://example.com")
        whenever(mockWebDavRepository.connect(config)).thenReturn(Result.success(Unit))
        whenever(mockWebDavRepository.listFiles("/", false)).thenReturn(Result.success(emptyList()))
        // 开启 showHidden 后数据层返回含隐藏文件的列表
        val filesWithHidden = listOf(
            WebDAVResource(path = "/.hidden_dir", name = ".hidden_dir", isDirectory = true),
            WebDAVResource(path = "/.DS_Store", name = ".DS_Store", isDirectory = false),
            WebDAVResource(path = "/file.txt", name = "file.txt", isDirectory = false)
        )
        whenever(mockWebDavRepository.listFiles("/", true)).thenReturn(Result.success(filesWithHidden))

        viewModel.selectServer(config)
        viewModel.toggleShowHidden()

        val names = viewModel.uiState.value.files.map { it.name }
        assertTrue("showHidden=true 时应包含隐藏文件: $names", names.contains(".DS_Store"))
        assertTrue(names.contains(".hidden_dir"))
        assertTrue(names.contains("file.txt"))
    }

    @Test
    fun `sort order preserved when hidden files are shown`() = runTest {
        val config = ServerConfig(name = "Test", url = "https://example.com")
        whenever(mockWebDavRepository.connect(config)).thenReturn(Result.success(Unit))
        whenever(mockWebDavRepository.listFiles("/", false)).thenReturn(Result.success(emptyList()))
        val filesWithHidden = listOf(
            WebDAVResource(path = "/.hidden_dir", name = ".hidden_dir", isDirectory = true),
            WebDAVResource(path = "/file.txt", name = "file.txt", isDirectory = false),
            WebDAVResource(path = "/.DS_Store", name = ".DS_Store", isDirectory = false)
        )
        whenever(mockWebDavRepository.listFiles("/", true)).thenReturn(Result.success(filesWithHidden))

        viewModel.selectServer(config)
        viewModel.toggleShowHidden()

        val state = viewModel.uiState.value
        // 排序规则：目录在前，随后文件按名称升序（大小写不敏感）
        // 目录：.hidden_dir；文件按 lowercase 升序：.DS_Store(.ds_store) < file.txt
        val expectedOrder = listOf(".hidden_dir", ".DS_Store", "file.txt")
        val actualNames = state.files.map { it.name }
        assertEquals("排序应按目录在前、名称升序", expectedOrder, actualNames)
    }

    // ========== clearError 测试 ==========

    @Test
    fun `clearError removes error from state`() = runTest {
        val config = ServerConfig(name = "Test", url = "https://example.com")
        whenever(mockWebDavRepository.connect(config))
            .thenReturn(Result.failure(WebDAVException.ConnectionFailed(Exception("test"))))

        viewModel.selectServer(config)
        assertNotNull(viewModel.uiState.value.error)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.errorInfo)
    }
}
