package com.tdull.webdavviewer.app.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdull.webdavviewer.app.data.model.BrowserLayoutMode
import com.tdull.webdavviewer.app.data.model.BrowserLayoutSettings
import com.tdull.webdavviewer.app.data.model.DownloadItem
import com.tdull.webdavviewer.app.data.model.DownloadState
import com.tdull.webdavviewer.app.data.model.FavoriteItem
import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.data.model.UploadConflictPolicy
import com.tdull.webdavviewer.app.data.model.UploadFileCandidate
import com.tdull.webdavviewer.app.data.model.UploadStatus
import com.tdull.webdavviewer.app.data.model.WebDAVException
import com.tdull.webdavviewer.app.data.model.WebDAVResource
import com.tdull.webdavviewer.app.data.repository.BrowserLayoutSettingsRepository
import com.tdull.webdavviewer.app.data.repository.ConfigRepository
import com.tdull.webdavviewer.app.data.repository.FavoritesRepository
import com.tdull.webdavviewer.app.data.repository.DownloadsRepository
import com.tdull.webdavviewer.app.data.repository.UploadsRepository
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
    val isNetworkAvailable: Boolean = true,
    val isOperationLoading: Boolean = false,
    val operationError: String? = null,
    val operationSuccess: String? = null,
    val showHidden: Boolean = false,
    val layoutMode: BrowserLayoutMode = BrowserLayoutMode.GRID,
    val gridColumns: Int = BrowserLayoutSettings.DEFAULT_GRID_COLUMNS,
    val isPreparingUpload: Boolean = false,
    val pendingUploadBatch: PendingUploadBatch? = null
)

data class PendingUploadBatch(
    val serverConfig: ServerConfig,
    val targetDirectory: String,
    val files: List<UploadFileCandidate>,
    val conflictFileNames: Set<String>,
    val blockedFileNames: Set<String>,
    val duplicateFileNames: Set<String>
) {
    val uploadableCount: Int
        get() = files.size - blockedFileNames.size
}

/**
 * 文件浏览器ViewModel
 */
@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val application: Application,
    private val webDavRepository: WebDAVRepository,
    private val configRepository: ConfigRepository,
    private val browserLayoutSettingsRepository: BrowserLayoutSettingsRepository,
    private val networkMonitor: NetworkMonitor,
    private val favoritesRepository: FavoritesRepository,
    private val downloadsRepository: DownloadsRepository,
    private val uploadsRepository: UploadsRepository,
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

    private val observedCompletedUploadIds = mutableSetOf<String>()

    // 视频预览图缓存：Map<视频路径, 预览图URL列表>
    private val _videoPreviews = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val videoPreviews: StateFlow<Map<String, List<String>>> = _videoPreviews.asStateFlow()
    
    // 收藏状态：Map<资源路径, 是否收藏>
    private val _favoriteStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val favoriteStates: StateFlow<Map<String, Boolean>> = _favoriteStates.asStateFlow()

    // 下载状态：Map<资源路径, 下载状态>
    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

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

        // 监听文件浏览器布局设置
        viewModelScope.launch {
            browserLayoutSettingsRepository.getLayoutSettings().collect { settings ->
                _uiState.update {
                    it.copy(
                        layoutMode = settings.layoutMode,
                        gridColumns = BrowserLayoutSettings.normalizeGridColumns(settings.gridColumns)
                    )
                }
            }
        }

        // 监听下载进度，自动更新下载状态
        viewModelScope.launch {
            downloadManager.downloadProgress.collect { progressMap ->
                _downloadStates.update { current ->
                    val updated = current.toMutableMap()
                    for ((path, progress) in progressMap) {
                        updated[path] = when {
                            progress.isDownloading -> DownloadState.Downloading(progress.progressPercent)
                            progress.isComplete -> DownloadState.Downloaded
                            progress.isFailed -> DownloadState.Error(progress.error ?: "下载失败")
                            else -> updated[path] ?: DownloadState.NotDownloaded
                        }
                    }
                    updated
                }
            }
        }

        // 上传完成后，如果浏览器正在显示同一个服务器/目录，自动刷新当前列表。
        viewModelScope.launch {
            uploadsRepository.uploads.collect { uploads ->
                val completedTasks = uploads.filter { it.status == UploadStatus.COMPLETED }
                val newlyCompleted = completedTasks.filter { observedCompletedUploadIds.add(it.id) }
                val currentServerId = currentServerConfig?.id
                val currentDirectory = normalizeDirectoryPath(_currentPath.value)
                if (newlyCompleted.any { it.serverId == currentServerId && it.targetDirectory == currentDirectory }) {
                    refresh()
                }
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
     * 切换是否显示隐藏文件，切换后重新加载当前目录
     */
    fun toggleShowHidden() {
        _uiState.update { it.copy(showHidden = !it.showHidden) }
        // 缓存键已区分 showHidden，切换后 loadFiles 会以新值重新拉取对应变体数据
        loadFiles(_currentPath.value)
    }

    /**
     * 切换文件列表布局模式
     */
    fun toggleLayoutMode() {
        val nextMode = if (_uiState.value.layoutMode == BrowserLayoutMode.GRID) {
            BrowserLayoutMode.LIST
        } else {
            BrowserLayoutMode.GRID
        }

        viewModelScope.launch {
            browserLayoutSettingsRepository.saveLayoutMode(nextMode)
        }
    }

    /**
     * 增加网格列数
     */
    fun increaseGridColumns() {
        val nextColumns = BrowserLayoutSettings.normalizeGridColumns(_uiState.value.gridColumns + 1)
        viewModelScope.launch {
            browserLayoutSettingsRepository.saveGridColumns(nextColumns)
        }
    }

    /**
     * 减少网格列数
     */
    fun decreaseGridColumns() {
        val nextColumns = BrowserLayoutSettings.normalizeGridColumns(_uiState.value.gridColumns - 1)
        viewModelScope.launch {
            browserLayoutSettingsRepository.saveGridColumns(nextColumns)
        }
    }

    /**
     * 获取流媒体URL
     */
    fun getStreamUrl(path: String): String {
        return webDavRepository.getStreamUrl(path)
    }

    /**
     * 获取图片缩略图URL
     */
    fun getImageThumbnailUrl(path: String): String {
        return webDavRepository.getImageThumbnailUrl(path)
    }

    /**
     * 获取图片中等缩略图URL
     */
    fun getImageMediumThumbnailUrl(path: String): String {
        return webDavRepository.getImageMediumThumbnailUrl(path)
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
            
            val result = webDavRepository.listFiles(path, showHidden = _uiState.value.showHidden)
            
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
            val progressMap = downloadManager.downloadProgress.value
            _downloadStates.update { current ->
                val updated = current.toMutableMap()
                paths.forEach { path ->
                    val progress = progressMap[path]
                    updated[path] = when {
                        progress?.isDownloading == true -> DownloadState.Downloading(progress.progressPercent)
                        progress?.isFailed == true -> DownloadState.Error(progress.error ?: "下载失败")
                        downloadedPaths.contains(path) -> DownloadState.Downloaded
                        else -> DownloadState.NotDownloaded
                    }
                }
                updated
            }
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
        }
    }

    /**
     * 重试下载
     */
    fun retryDownload(resourcePath: String) {
        viewModelScope.launch {
            downloadManager.retryDownload(resourcePath)
        }
    }

    /**
     * 取消下载
     */
    fun cancelDownload(resourcePath: String) {
        downloadManager.cancelDownload(resourcePath)
    }

    /**
     * 重命名文件或文件夹
     * @param resource 要重命名的资源
     * @param newName 新的名称
     */
    fun renameResource(resource: WebDAVResource, newName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOperationLoading = true, operationError = null, operationSuccess = null) }
            val result = webDavRepository.rename(resource, newName)
            handleOperationResult(
                result = result,
                successMessage = "已重命名为 \"$newName\""
            )
        }
    }

    /**
     * 移动文件或文件夹到目标目录
     * @param resource 要移动的资源
     * @param destinationDir 目标目录路径
     */
    fun moveResource(resource: WebDAVResource, destinationDir: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOperationLoading = true, operationError = null, operationSuccess = null) }
            val result = webDavRepository.move(resource, destinationDir)
            handleOperationResult(
                result = result,
                successMessage = "已移动到 \"${destinationDir.trimEnd('/')}\""
            )
        }
    }

    /**
     * 删除文件或文件夹
     * @param resource 要删除的资源
     */
    fun deleteResource(resource: WebDAVResource) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOperationLoading = true, operationError = null, operationSuccess = null) }
            val result = webDavRepository.delete(resource)
            handleOperationResult(
                result = result,
                successMessage = "已删除 \"${resource.name}\""
            )
        }
    }

    /**
     * 在当前目录创建文件夹
     * @param folderName 新文件夹名称
     */
    fun createDirectory(folderName: String) {
        val trimmedName = folderName.trim()
        viewModelScope.launch {
            _uiState.update { it.copy(isOperationLoading = true, operationError = null, operationSuccess = null) }
            val result = webDavRepository.createDirectory(_currentPath.value, trimmedName)
            handleOperationResult(
                result = result,
                successMessage = "已创建文件夹 \"$trimmedName\""
            )
        }
    }

    /**
     * 准备一批系统文件选择器返回的文件。
     */
    fun prepareUploads(uris: List<Uri>) {
        val serverConfig = currentServerConfig
        if (serverConfig == null || !_uiState.value.isConnected) {
            _uiState.update { it.copy(operationError = "请先连接服务器后再上传") }
            return
        }

        if (uris.isEmpty()) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPreparingUpload = true,
                    operationError = null,
                    operationSuccess = null,
                    pendingUploadBatch = null
                )
            }

            val candidates = uris
                .distinctBy { it.toString() }
                .mapNotNull { uri ->
                    takePersistableReadPermission(uri)
                    readUploadCandidate(uri)
                }

            if (candidates.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isPreparingUpload = false,
                        operationError = "无法读取所选文件"
                    )
                }
                return@launch
            }

            val duplicateFileNames = candidates
                .groupBy { it.fileName }
                .filterValues { it.size > 1 }
                .keys
                .toSet()
            val dedupedCandidates = candidates
                .groupBy { it.fileName }
                .map { it.value.first() }

            val remoteFilesResult = webDavRepository.listFiles(_currentPath.value, showHidden = true)
            remoteFilesResult.fold(
                onSuccess = { remoteFiles ->
                    val remoteFileNames = remoteFiles
                        .filterNot { it.isDirectory }
                        .map { it.name }
                        .toSet()
                    val remoteDirectoryNames = remoteFiles
                        .filter { it.isDirectory }
                        .map { it.name }
                        .toSet()

                    val blockedFileNames = dedupedCandidates
                        .filter { it.fileName in remoteDirectoryNames }
                        .map { it.fileName }
                        .toSet()
                    val uploadableCandidates = dedupedCandidates
                        .filterNot { it.fileName in blockedFileNames }
                    val conflictFileNames = uploadableCandidates
                        .filter { it.fileName in remoteFileNames }
                        .map { it.fileName }
                        .toSet()

                    if (uploadableCandidates.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isPreparingUpload = false,
                                operationError = "所选文件均与远端文件夹重名，无法上传"
                            )
                        }
                        return@fold
                    }

                    _uiState.update {
                        it.copy(
                            isPreparingUpload = false,
                            pendingUploadBatch = PendingUploadBatch(
                                serverConfig = serverConfig,
                                targetDirectory = normalizeDirectoryPath(_currentPath.value),
                                files = dedupedCandidates,
                                conflictFileNames = conflictFileNames,
                                blockedFileNames = blockedFileNames,
                                duplicateFileNames = duplicateFileNames
                            )
                        )
                    }
                },
                onFailure = { error ->
                    val errorInfo = ErrorHandler.getErrorInfo(error, application)
                    _uiState.update {
                        it.copy(
                            isPreparingUpload = false,
                            operationError = "检查远端同名文件失败：${errorInfo.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * 入队当前等待确认的上传批次。
     */
    fun enqueuePendingUploads(conflictPolicy: UploadConflictPolicy) {
        val batch = _uiState.value.pendingUploadBatch ?: return

        val filesToUpload = batch.files
            .filterNot { it.fileName in batch.blockedFileNames }
            .filterNot { it.fileName in batch.conflictFileNames && conflictPolicy == UploadConflictPolicy.SKIP }

        if (filesToUpload.isEmpty()) {
            _uiState.update {
                it.copy(
                    pendingUploadBatch = null,
                    operationSuccess = "已跳过所有同名文件",
                    operationError = null
                )
            }
            return
        }

        val conflictPolicies = filesToUpload.associate { candidate ->
            candidate.fileName to if (candidate.fileName in batch.conflictFileNames) {
                conflictPolicy
            } else {
                UploadConflictPolicy.SKIP
            }
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isOperationLoading = true,
                    operationError = null,
                    operationSuccess = null
                )
            }

            val result = uploadsRepository.enqueueUploads(
                files = filesToUpload,
                serverConfig = batch.serverConfig,
                targetDirectory = batch.targetDirectory,
                conflictPolicies = conflictPolicies
            )

            result.fold(
                onSuccess = { tasks ->
                    val skipped = batch.uploadableCount - tasks.size
                    val message = if (skipped > 0) {
                        "已加入上传队列 ${tasks.size} 个文件，跳过 $skipped 个同名文件"
                    } else {
                        "已加入上传队列 ${tasks.size} 个文件"
                    }
                    _uiState.update {
                        it.copy(
                            isOperationLoading = false,
                            pendingUploadBatch = null,
                            operationSuccess = message,
                            operationError = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isOperationLoading = false,
                            operationError = error.message ?: "创建上传任务失败",
                            operationSuccess = null
                        )
                    }
                }
            )
        }
    }

    fun dismissPendingUploads() {
        _uiState.update { it.copy(pendingUploadBatch = null) }
    }

    /**
     * 清除操作状态（成功/错误提示）
     */
    fun clearOperationFeedback() {
        _uiState.update { it.copy(operationError = null, operationSuccess = null) }
    }

    /**
     * 列出指定路径下的子目录（用于移动对话框选择目标目录）
     * @param path 目录路径
     * @return 子目录列表；失败时返回空列表
     */
    suspend fun listDirectories(path: String): List<WebDAVResource> {
        return try {
            val result = webDavRepository.listFiles(path)
            result.getOrNull()
                ?.filter { it.isDirectory }
                ?.filter { it.path.trimEnd('/') != path.trimEnd('/') }
                ?.sortedBy { it.name.lowercase() }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 处理操作结果：成功后刷新当前目录，失败则记录错误
     */
    private fun handleOperationResult(
        result: Result<Unit>,
        successMessage: String
    ) {
        result.fold(
            onSuccess = {
                _uiState.update {
                    it.copy(
                        isOperationLoading = false,
                        operationSuccess = successMessage,
                        operationError = null
                    )
                }
                // 操作成功后刷新当前目录
                refresh()
            },
            onFailure = { error ->
                val errorInfo = ErrorHandler.getErrorInfo(error, application)
                _uiState.update {
                    it.copy(
                        isOperationLoading = false,
                        operationError = errorInfo.message,
                        operationSuccess = null
                    )
                }
            }
        )
    }

    private fun takePersistableReadPermission(uri: Uri) {
        runCatching {
            application.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    private fun readUploadCandidate(uri: Uri): UploadFileCandidate? {
        val resolver = application.contentResolver
        var displayName: String? = null
        var size = -1L

        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex)
                }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }

        val name = displayName
            ?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
            ?: return null

        return UploadFileCandidate(
            uriString = uri.toString(),
            fileName = name.replace("/", "_").replace("\\", "_"),
            mimeType = resolver.getType(uri),
            fileSize = size
        )
    }

    private fun normalizeDirectoryPath(path: String): String {
        val normalized = path.ifBlank { "/" }.let { if (it.startsWith("/")) it else "/$it" }
        return if (normalized == "/") "/" else "${normalized.trimEnd('/')}/"
    }
}
