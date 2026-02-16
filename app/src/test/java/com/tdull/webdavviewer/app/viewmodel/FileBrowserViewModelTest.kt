package com.tdull.webdavviewer.app.viewmodel

import android.app.Application
import app.cash.turbine.test
import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.data.model.WebDAVException
import com.tdull.webdavviewer.app.data.model.WebDAVResource
import com.tdull.webdavviewer.app.data.repository.ConfigRepository
import com.tdull.webdavviewer.app.data.repository.WebDAVRepository
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
import org.mockito.kotlin.any
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
        whenever(mockWebDavRepository.connect(any())).thenReturn(Result.success(Unit))

        viewModel = FileBrowserViewModel(
            application = mockApplication,
            webDavRepository = mockWebDavRepository,
            configRepository = mockConfigRepository,
            networkMonitor = mockNetworkMonitor
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
        whenever(mockWebDavRepository.listFiles("/")).thenReturn(Result.success(files))

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

        viewModel.uiState.test {
            val finalState = awaitItem()
            assertFalse(finalState.isConnected)
            assertFalse(finalState.isNetworkAvailable)
            assertNotNull(finalState.error)
        }
    }

    // ========== navigateTo 测试 ==========

    @Test
    fun `navigateTo updates currentPath and loads files`() = runTest {
        val files = listOf(
            WebDAVResource(path = "/subfolder/file.txt", name = "file.txt", isDirectory = false)
        )
        whenever(mockWebDavRepository.listFiles("/subfolder")).thenReturn(Result.success(files))

        // 先连接服务器
        val config = ServerConfig(name = "Test", url = "https://example.com")
        viewModel.selectServer(config)

        viewModel.navigateTo("/subfolder")

        assertEquals("/subfolder", viewModel.currentPath.value)
        verify(mockWebDavRepository).listFiles("/subfolder")
    }

    // ========== navigateUp 测试 ==========

    @Test
    fun `navigateUp returns to previous path`() = runTest {
        whenever(mockWebDavRepository.listFiles(any())).thenReturn(Result.success(emptyList()))

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
        whenever(mockWebDavRepository.listFiles(any())).thenReturn(Result.success(emptyList()))

        val config = ServerConfig(name = "Test", url = "https://example.com")
        viewModel.selectServer(config)

        viewModel.navigateUp()

        assertEquals("/", viewModel.currentPath.value)
    }

    // ========== refresh 测试 ==========

    @Test
    fun `refresh reloads current directory`() = runTest {
        whenever(mockWebDavRepository.listFiles("/")).thenReturn(Result.success(emptyList()))

        val config = ServerConfig(name = "Test", url = "https://example.com")
        viewModel.selectServer(config)

        viewModel.refresh()

        verify(mockWebDavRepository).listFiles("/")
    }

    // ========== getStreamUrl 测试 ==========

    @Test
    fun `getStreamUrl returns correct URL`() {
        val expectedUrl = "https://example.com/video.mp4"
        whenever(mockWebDavRepository.getStreamUrl("/video.mp4")).thenReturn(expectedUrl)

        val result = viewModel.getStreamUrl("/video.mp4")

        assertEquals(expectedUrl, result)
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
