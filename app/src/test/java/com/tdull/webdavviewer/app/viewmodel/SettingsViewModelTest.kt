package com.tdull.webdavviewer.app.viewmodel

import android.app.Application
import app.cash.turbine.test
import com.tdull.webdavviewer.app.data.model.ServerConfig
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
 * SettingsViewModel 单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @Mock
    private lateinit var mockApplication: Application

    @Mock
    private lateinit var mockConfigRepository: ConfigRepository

    @Mock
    private lateinit var mockWebDavRepository: WebDAVRepository

    @Mock
    private lateinit var mockNetworkMonitor: NetworkMonitor

    private lateinit var viewModel: SettingsViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // 默认配置
        whenever(mockConfigRepository.servers).thenReturn(flowOf(emptyList()))
        whenever(mockConfigRepository.activeServerId).thenReturn(flowOf(null))
        whenever(mockNetworkMonitor.networkStatus).thenReturn(flowOf(NetworkStatus(isAvailable = true)))
        whenever(mockNetworkMonitor.isNetworkAvailable()).thenReturn(true)

        viewModel = SettingsViewModel(
            application = mockApplication,
            configRepository = mockConfigRepository,
            webDavRepository = mockWebDavRepository,
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
        assertTrue(initialState.servers.isEmpty())
        assertNull(initialState.activeServerId)
        assertNull(initialState.editingServer)
        assertFalse(initialState.showAddDialog)
        assertFalse(initialState.showEditDialog)
        assertFalse(initialState.showDeleteConfirm)
        assertNull(initialState.testConnectionResult)
        assertNull(initialState.error)
        assertTrue(initialState.isNetworkAvailable)
    }

    // ========== 对话框状态测试 ==========

    @Test
    fun `showAddDialog sets showAddDialog to true`() = runTest {
        viewModel.showAddDialog()
        
        assertTrue(viewModel.uiState.value.showAddDialog)
        assertNull(viewModel.uiState.value.editingServer)
    }

    @Test
    fun `hideAddDialog resets dialog state`() = runTest {
        viewModel.showAddDialog()
        viewModel.hideAddDialog()
        
        assertFalse(viewModel.uiState.value.showAddDialog)
        assertNull(viewModel.uiState.value.editingServer)
        assertNull(viewModel.uiState.value.testConnectionResult)
    }

    @Test
    fun `showEditDialog sets editingServer and showEditDialog`() = runTest {
        val server = ServerConfig(
            id = "test-id",
            name = "Test Server",
            url = "https://example.com"
        )
        
        viewModel.showEditDialog(server)
        
        assertTrue(viewModel.uiState.value.showEditDialog)
        assertEquals(server, viewModel.uiState.value.editingServer)
    }

    @Test
    fun `hideEditDialog resets edit dialog state`() = runTest {
        val server = ServerConfig(name = "Test", url = "https://example.com")
        viewModel.showEditDialog(server)
        viewModel.hideEditDialog()
        
        assertFalse(viewModel.uiState.value.showEditDialog)
        assertNull(viewModel.uiState.value.editingServer)
    }

    @Test
    fun `showDeleteConfirm sets serverToDelete`() = runTest {
        val server = ServerConfig(
            id = "delete-id",
            name = "To Delete",
            url = "https://example.com"
        )
        
        viewModel.showDeleteConfirm(server)
        
        assertTrue(viewModel.uiState.value.showDeleteConfirm)
        assertEquals(server, viewModel.uiState.value.serverToDelete)
    }

    @Test
    fun `hideDeleteConfirm resets delete state`() = runTest {
        val server = ServerConfig(name = "Test", url = "https://example.com")
        viewModel.showDeleteConfirm(server)
        viewModel.hideDeleteConfirm()
        
        assertFalse(viewModel.uiState.value.showDeleteConfirm)
        assertNull(viewModel.uiState.value.serverToDelete)
    }

    // ========== saveServer 测试 ==========

    @Test
    fun `saveServer adds server and hides dialog`() = runTest {
        val server = ServerConfig(
            name = "Test Server",
            url = "https://example.com"
        )

        viewModel.showAddDialog()
        viewModel.saveServer(server)

        verify(mockConfigRepository).addServer(server)
        assertFalse(viewModel.uiState.value.showAddDialog)
    }

    // ========== deleteServer 测试 ==========

    @Test
    fun `deleteServer removes server and hides confirm dialog`() = runTest {
        val server = ServerConfig(
            id = "delete-id",
            name = "To Delete",
            url = "https://example.com"
        )

        viewModel.showDeleteConfirm(server)
        viewModel.deleteServer()

        verify(mockConfigRepository).removeServer("delete-id")
        assertFalse(viewModel.uiState.value.showDeleteConfirm)
    }

    // ========== setActiveServer 测试 ==========

    @Test
    fun `setActiveServer calls repository`() = runTest {
        val serverId = "test-server-id"

        viewModel.setActiveServer(serverId)

        verify(mockConfigRepository).setActiveServer(serverId)
    }

    // ========== testConnection 测试 ==========

    @Test
    fun `testConnection returns Success when connection succeeds`() = runTest {
        val config = ServerConfig(name = "Test", url = "https://example.com")
        whenever(mockWebDavRepository.testConnection(config)).thenReturn(Result.success(true))

        viewModel.testConnection(config)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.testConnectionResult is TestConnectionResult.Success)
        }
    }

    @Test
    fun `testConnection returns Failed when connection fails`() = runTest {
        val config = ServerConfig(name = "Test", url = "https://example.com")
        whenever(mockWebDavRepository.testConnection(config)).thenReturn(Result.success(false))

        viewModel.testConnection(config)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.testConnectionResult is TestConnectionResult.Failed)
        }
    }

    @Test
    fun `testConnection returns Failed with error when exception`() = runTest {
        val config = ServerConfig(name = "Test", url = "https://example.com")
        whenever(mockWebDavRepository.testConnection(config))
            .thenReturn(Result.failure(Exception("Connection error")))

        viewModel.testConnection(config)

        viewModel.uiState.test {
            val state = awaitItem()
            val result = state.testConnectionResult
            assertTrue(result is TestConnectionResult.Failed)
            assertTrue((result as TestConnectionResult.Failed).message.contains("Connection error"))
        }
    }

    @Test
    fun `testConnection returns Failed when network unavailable`() = runTest {
        val config = ServerConfig(name = "Test", url = "https://example.com")
        whenever(mockNetworkMonitor.isNetworkAvailable()).thenReturn(false)

        viewModel.testConnection(config)

        viewModel.uiState.test {
            val state = awaitItem()
            val result = state.testConnectionResult
            assertTrue(result is TestConnectionResult.Failed)
            assertTrue((result as TestConnectionResult.Failed).message.contains("无网络连接"))
        }
    }

    // ========== clearError 测试 ==========

    @Test
    fun `clearError removes error from state`() = runTest {
        // 触发一个错误
        val config = ServerConfig(name = "Test", url = "https://example.com")
        whenever(mockWebDavRepository.testConnection(config))
            .thenReturn(Result.failure(Exception("Test error")))
        viewModel.testConnection(config)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.errorInfo)
    }

    @Test
    fun `clearTestResult removes testConnectionResult from state`() = runTest {
        val config = ServerConfig(name = "Test", url = "https://example.com")
        whenever(mockWebDavRepository.testConnection(config)).thenReturn(Result.success(true))
        viewModel.testConnection(config)

        viewModel.clearTestResult()

        assertNull(viewModel.uiState.value.testConnectionResult)
    }
}
