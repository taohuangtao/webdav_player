package com.tdull.webdavviewer.app.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdull.webdavviewer.app.data.model.FavoriteItem
import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.data.model.WebDAVException
import com.tdull.webdavviewer.app.data.model.WebDAVResource
import com.tdull.webdavviewer.app.data.repository.ConfigRepository
import com.tdull.webdavviewer.app.data.repository.FavoritesRepository
import com.tdull.webdavviewer.app.data.repository.DownloadsRepository
import com.tdull.webdavviewer.app.data.repository.WebDAVRepository
import com.tdull.webdavviewer.app.service.DownloadManager
import com.tdull.webdavviewer.app.service.DownloadProgress
import com.tdull.webdavviewer.app.util.ErrorHandler
import com.tdull.webdavviewer.app.util.ErrorInfo
import com.tdull.webdavviewer.app.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 文件浏览器UI状态
 */
data class FileBrowserUiState(
    val isLoading: Boolean = false,
    val files: List<WebDAVResource> = emptyList(),
    val error: String? = null,
    val errorInfo: ErrorInfo? = null,
    val isConnected: Boolean = false,
    val currentServer: ServerConfig? = null,
    val isNetworkAvailable: Boolean = true
)

/**
 * 文件浏览器ViewModel
 */
@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val application: Application,
    private val webDavRepository: WebDAVRepository,
    private val configRepository: ConfigRepository,
    private val networkMonitor: NetworkMonitor,
    private val favoritesRepository: FavoritesRepository,
    private val downloadsRepository: DownloadsRepository,
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileBrowserUiState())
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    // 路径历史栈，用于返回上一级
    private val pathStack = mutableListOf<String>()

    // 当前服务器配置
    private var currentServerConfig: ServerConfig? = null

    // 视频预览图缓存：Map<视频路径, 预览图URL列表>
    private val _videoPreviews = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val videoPreviews: StateFlow<Map<String, List<String>>> = _videoPreviews.asStateFlow()
    
    // 收藏状态：Map<资源路径, 是否收藏>
    private val _favoriteStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val favoriteStates: StateFlow<Map<String, Boolean>> = _favoriteStates.asStateFlow()

    // 下载状态：Map<资源路径, 是否已下载>
    private val _downloadStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val downloadStates: StateFlow<Map<String, Boolean>> = _downloadStates.asStateFlow()

    // 下载进度：Map<资源路径, 进度信息>
    val downloadProgress: StateFlow<Map<String, DownloadProgress>> = downloadManager.downloadProgress

    // 激活的服务器
    val activeServer: StateFlow<ServerConfig?> = configRepository.activeServer
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        // 监听网络状态
        viewModelScope.launch {
            networkMonitor.networkStatus.collect { status ->
                _uiState.update { it.copy(isNetworkAvailable = status.isAvailable) }
            }
        }
    }

    /**
     * 根据服务器ID选择服务器
     * 如果已经连接到该服务器，则不执行任何操作（保留当前浏览状态）
     */
    fun selectServerById(serverId: String) {
        viewModelScope.launch {
            val server = configRepository.servers.first()
                .find { it.id == serverId }
            server?.let {
                // 如果已经连接到同一个服务器，则跳过（保持当前浏览状态）
                if (currentServerConfig?.id == it.id && _uiState.value.isConnected) {
                    return@launch
                }
                selectServer(it)
            }
        }
    }

    /**
     * 选择服务器并连接
     */
    fun selectServer(config: ServerConfig) {
        // 如果是同一个服务器且已连接，则不重复连接（保持当前浏览状态）
        val isSameServer = currentServerConfig?.id == config.id
        if (isSameServer && _uiState.value.isConnected) {
            return
        }

        // 先检查网络状态
        if (!networkMonitor.isNetworkAvailable()) {
            _uiState.update {
                it.copy(
                    isConnected = false,
                    isLoading = false,
                    errorInfo = ErrorInfo(
                        type = com.tdull.webdavviewer.app.util.ErrorType.NETWORK_UNAVAILABLE,
                        title = "无网络连接",
                        message = "请检查您的网络连接后重试",
                        canRetry = true
                    ),
                    error = "无网络连接"
                )
            }
            return
        }

        currentServerConfig = config
        _uiState.update { it.copy(currentServer = config) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, errorInfo = null) }

            val result = webDavRepository.connect(config)

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isConnected = true, isLoading = false) }
                    // 只有首次连接或切换服务器时才重置到根目录
                    // 如果是同一服务器的重复连接（如网络中断后重连），保持当前浏览状态
                    if (!isSameServer) {
                        _currentPath.value = "/"
                        pathStack.clear()
                        loadFiles("/")
                    }
                },
                onFailure = { error ->
                    val errorInfo = ErrorHandler.getErrorInfo(error, application)
                    _uiState.update {
                        it.copy(
                            isConnected = false,
                            isLoading = false,
                            errorInfo = errorInfo,
                            error = errorInfo.message
                        )
                    }
                }
            )
        }
    }

    /**
     * 导航到指定路径
     */
    fun navigateTo(path: String) {
        // 保存当前路径到历史栈
        pathStack.add(_currentPath.value)
        
        _currentPath.value = path
        loadFiles(path)
    }

    /**
     * 返回上一级目录
     */
    fun navigateUp() {
        if (pathStack.isNotEmpty()) {
            val previousPath = pathStack.removeAt(pathStack.size - 1)
            _currentPath.value = previousPath
            loadFiles(previousPath)
        } else if (_currentPath.value != "/") {
            // 如果历史栈为空但不是根目录，则返回上级目录
            val currentPath = _currentPath.value
            val parentPath = getParentPath(currentPath)
            _currentPath.value = parentPath
            loadFiles(parentPath)
        }
    }

    /**
     * 刷新当前目录
     */
    fun refresh() {
        viewModelScope.launch {
            // 清除缓存后重新加载
            (webDavRepository as? com.tdull.webdavviewer.app.data.repository.WebDAVRepositoryImpl)?.clearCache(_currentPath.value)
            loadFiles(_currentPath.value)
        }
    }

    /**
     * 获取流媒体URL
     */
    fun getStreamUrl(path: String): String {
        return webDavRepository.getStreamUrl(path)
    }
    
    /**
     * 加载视频预览图
     */
    fun loadVideoPreviews(videoPath: String) {
        // 如果已经缓存，则不再重复加载
        if (_videoPreviews.value.containsKey(videoPath)) {
            return
        }
        
        viewModelScope.launch {
            val result = webDavRepository.getVideoPreviews(videoPath)
            result.fold(
                onSuccess = { previews ->
                    if (previews.isNotEmpty()) {
                        _videoPreviews.update { it + (videoPath to previews) }
                    }
                },
                onFailure = {
                    // 静默失败，不影响主流程
                }
            )
        }
    }
    
    /**
     * 获取视频预览图列表
     */
    fun getVideoPreviewList(videoPath: String): List<String> {
        return _videoPreviews.value[videoPath] ?: emptyList()
    }

    /**
     * 加载文件列表
     */
    private fun loadFiles(path: String) {
        // 检查网络状态
        if (!networkMonitor.isNetworkAvailable()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorInfo = ErrorInfo(
                        type = com.tdull.webdavviewer.app.util.ErrorType.NETWORK_UNAVAILABLE,
                        title = "无网络连接",
                        message = "请检查您的网络连接后重试",
                        canRetry = true
                    ),
                    error = "无网络连接"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, errorInfo = null) }
            
            val result = webDavRepository.listFiles(path)
            
            result.fold(
                onSuccess = { files ->
                    // 过滤掉当前目录本身（WebDAV可能会返回当前目录）
                    // 标准化路径比较：统一移除尾部斜杠
                    val normalizedPath = path.trimEnd('/')
                    val filteredFiles = files.filter { 
                        it.path.trimEnd('/') != normalizedPath && it.name.isNotEmpty() 
                    }
                    // 排序：目录在前，然后按名称排序
                    val sortedFiles = filteredFiles.sortedWith(
                        compareBy<WebDAVResource> { !it.isDirectory }
                            .thenBy { it.name.lowercase() }
                    )
                    
                    _uiState.update { 
                        it.copy(
                            files = sortedFiles,
                            isLoading = false,
                            error = null,
                            errorInfo = null
                        ) 
                    }
                },
                onFailure = { error ->
                    val errorInfo = ErrorHandler.getErrorInfo(error, application)
                    _uiState.update { 
                        it.copy(
                            files = emptyList(),
                            isLoading = false,
                            errorInfo = errorInfo,
                            error = errorInfo.message
                        ) 
                    }
                }
            )
        }
    }

    /**
     * 获取父目录路径
     */
    private fun getParentPath(path: String): String {
        if (path == "/" || path.isEmpty()) return "/"
        
        val normalizedPath = path.trimEnd('/')
        val lastSlashIndex = normalizedPath.lastIndexOf('/')
        
        return if (lastSlashIndex <= 0) {
            "/"
        } else {
            normalizedPath.substring(0, lastSlashIndex + 1)
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null, errorInfo = null) }
    }
    
    /**
     * 切换收藏状态
     */
    fun toggleFavorite(resource: WebDAVResource) {
        val serverId = currentServerConfig?.id ?: return
        val videoUrl = webDavRepository.getStreamUrl(resource.path)
        val isCurrentlyFavorite = _favoriteStates.value[resource.path] ?: false
        
        viewModelScope.launch {
            if (isCurrentlyFavorite) {
                // 取消收藏
                val favorites = favoritesRepository.favorites.first()
                val existing = favorites.find { it.videoUrl == videoUrl }
                existing?.let { 
                    favoritesRepository.removeFavorite(it.id)
                    _favoriteStates.update { it - resource.path }
                }
            } else {
                // 添加收藏
                val newItem = FavoriteItem(
                    videoUrl = videoUrl,
                    videoTitle = resource.name,
                    serverId = serverId,
                    resourcePath = resource.path
                )
                favoritesRepository.addFavorite(newItem)
                _favoriteStates.update { it + (resource.path to true) }
            }
        }
    }
    
    /**
     * 检查并加载收藏状态
     */
    fun loadFavoriteStates(paths: List<String>) {
        viewModelScope.launch {
            val favorites = favoritesRepository.favorites.first()
            val favoritePaths = favorites.map { it.resourcePath }.toSet()
            val newStates = _favoriteStates.value.toMutableMap()
            paths.forEach { path ->
                newStates[path] = favoritePaths.contains(path)
            }
            _favoriteStates.value = newStates
        }
    }

    /**
     * 检查并加载下载状态
     */
    fun loadDownloadStates(paths: List<String>) {
        viewModelScope.launch {
            val downloads = downloadsRepository.downloads.first()
            val downloadedPaths = downloads.map { it.resourcePath }.toSet()
            val newStates = _downloadStates.value.toMutableMap()
            paths.forEach { path ->
                newStates[path] = downloadedPaths.contains(path)
            }
            _downloadStates.value = newStates
        }
    }

    /**
     * 开始下载视频文件
     */
    fun startDownload(resource: WebDAVResource) {
        val serverId = currentServerConfig?.id ?: return

        // 检查是否已在下载中
        val currentProgress = downloadProgress.value[resource.path]
        if (currentProgress?.isDownloading == true) {
            return
        }

        viewModelScope.launch {
            downloadManager.startDownload(resource, serverId)
                .onSuccess {
                    // 更新下载状态
                    _downloadStates.update { it + (resource.path to true) }
                }
        }
    }
}
