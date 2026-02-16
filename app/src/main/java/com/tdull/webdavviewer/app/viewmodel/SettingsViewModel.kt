package com.tdull.webdavviewer.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.data.repository.ConfigRepository
import com.tdull.webdavviewer.app.data.repository.WebDAVRepository
import com.tdull.webdavviewer.app.util.ErrorHandler
import com.tdull.webdavviewer.app.util.ErrorInfo
import com.tdull.webdavviewer.app.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置页面UI状态
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val servers: List<ServerConfig> = emptyList(),
    val activeServerId: String? = null,
    val editingServer: ServerConfig? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val serverToDelete: ServerConfig? = null,
    val testConnectionResult: TestConnectionResult? = null,
    val error: String? = null,
    val errorInfo: ErrorInfo? = null,
    val isNetworkAvailable: Boolean = true
)

/**
 * 连接测试结果
 */
sealed class TestConnectionResult {
    data class Success(val message: String = "连接成功") : TestConnectionResult()
    data class Failed(val message: String, val errorInfo: ErrorInfo? = null) : TestConnectionResult()
    object Testing : TestConnectionResult()
}

/**
 * 设置页面ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val configRepository: ConfigRepository,
    private val webDavRepository: WebDAVRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // 服务器列表
    val servers: StateFlow<List<ServerConfig>> = configRepository.servers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 当前激活的服务器ID
    val activeServerId: StateFlow<String?> = configRepository.activeServerId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        // 监听服务器列表变化
        viewModelScope.launch {
            configRepository.servers.collect { serverList ->
                _uiState.update { it.copy(servers = serverList) }
            }
        }

        // 监听激活服务器变化
        viewModelScope.launch {
            configRepository.activeServerId.collect { id ->
                _uiState.update { it.copy(activeServerId = id) }
            }
        }

        // 监听网络状态
        viewModelScope.launch {
            networkMonitor.networkStatus.collect { status ->
                _uiState.update { it.copy(isNetworkAvailable = status.isAvailable) }
            }
        }
    }

    /**
     * 显示添加服务器对话框
     */
    fun showAddDialog() {
        _uiState.update { 
            it.copy(
                showAddDialog = true,
                editingServer = null,
                testConnectionResult = null
            ) 
        }
    }

    /**
     * 隐藏添加服务器对话框
     */
    fun hideAddDialog() {
        _uiState.update { 
            it.copy(
                showAddDialog = false,
                editingServer = null,
                testConnectionResult = null
            ) 
        }
    }

    /**
     * 显示编辑服务器对话框
     */
    fun showEditDialog(server: ServerConfig) {
        _uiState.update { 
            it.copy(
                showEditDialog = true,
                editingServer = server,
                testConnectionResult = null
            ) 
        }
    }

    /**
     * 隐藏编辑服务器对话框
     */
    fun hideEditDialog() {
        _uiState.update { 
            it.copy(
                showEditDialog = false,
                editingServer = null,
                testConnectionResult = null
            ) 
        }
    }

    /**
     * 显示删除确认对话框
     */
    fun showDeleteConfirm(server: ServerConfig) {
        _uiState.update { 
            it.copy(
                showDeleteConfirm = true,
                serverToDelete = server
            ) 
        }
    }

    /**
     * 隐藏删除确认对话框
     */
    fun hideDeleteConfirm() {
        _uiState.update { 
            it.copy(
                showDeleteConfirm = false,
                serverToDelete = null
            ) 
        }
    }

    /**
     * 保存服务器配置
     */
    fun saveServer(config: ServerConfig) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, errorInfo = null) }
            try {
                configRepository.addServer(config)
                hideAddDialog()
                hideEditDialog()
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.getErrorInfo(e, application)
                _uiState.update { it.copy(error = errorInfo.message, errorInfo = errorInfo) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 删除服务器配置
     */
    fun deleteServer() {
        val server = _uiState.value.serverToDelete ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, errorInfo = null) }
            try {
                configRepository.removeServer(server.id)
                hideDeleteConfirm()
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.getErrorInfo(e, application)
                _uiState.update { it.copy(error = errorInfo.message, errorInfo = errorInfo) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 设置激活的服务器
     */
    fun setActiveServer(serverId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, errorInfo = null) }
            try {
                configRepository.setActiveServer(serverId)
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.getErrorInfo(e, application)
                _uiState.update { it.copy(error = errorInfo.message, errorInfo = errorInfo) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 测试服务器连接
     */
    fun testConnection(config: ServerConfig) {
        // 使用日志打印配置信息
        Log.d("SettingsViewModel", "Testing connection: $config")
        

        // 先检查网络状态
        if (!networkMonitor.isNetworkAvailable()) {
            _uiState.update { 
                it.copy(
                    testConnectionResult = TestConnectionResult.Failed(
                        message = "无网络连接，请检查网络设置",
                        errorInfo = ErrorInfo(
                            type = com.tdull.webdavviewer.app.util.ErrorType.NETWORK_UNAVAILABLE,
                            title = "无网络连接",
                            message = "请检查您的网络连接后重试",
                            canRetry = true
                        )
                    )
                ) 
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(testConnectionResult = TestConnectionResult.Testing) }
            
            val result = webDavRepository.testConnection(config)
            
            result.fold(
                onSuccess = { success ->
                    Log.d("onSuccess", "onSuccess: $success")
                    if (success) {
                        _uiState.update { 
                            it.copy(testConnectionResult = TestConnectionResult.Success()) 
                        }
                    } else {
                        _uiState.update { 
                            it.copy(testConnectionResult = TestConnectionResult.Failed("连接失败")) 
                        }
                    }
                },
                onFailure = { error ->
                    Log.d("onFailure", "onFailure: $config")
                    val errorInfo = ErrorHandler.getErrorInfo(error, application)
                    _uiState.update { 
                        it.copy(testConnectionResult = TestConnectionResult.Failed(
                            message = errorInfo.message,
                            errorInfo = errorInfo
                        )) 
                    }
                }
            )
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null, errorInfo = null) }
    }

    /**
     * 清除连接测试结果
     */
    fun clearTestResult() {
        _uiState.update { it.copy(testConnectionResult = null) }
    }
}
