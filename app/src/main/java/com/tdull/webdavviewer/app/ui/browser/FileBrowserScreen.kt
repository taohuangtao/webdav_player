package com.tdull.webdavviewer.app.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import android.widget.Toast
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.tdull.webdavviewer.app.data.model.DownloadState
import com.tdull.webdavviewer.app.data.model.WebDAVResource
import com.tdull.webdavviewer.app.viewmodel.FileBrowserViewModel

/**
 * 文件浏览器页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    viewModel: FileBrowserViewModel = hiltViewModel(),
    serverId: String? = null,
    onVideoClick: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val videoPreviews by viewModel.videoPreviews.collectAsState()
    val favoriteStates by viewModel.favoriteStates.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val context = LocalContext.current
    
    // 全屏预览图状态
    var previewState by remember { mutableStateOf<PreviewState?>(null) }
    
    // 操作菜单状态：当前选中的资源（弹出更多菜单）
    var menuResource by remember { mutableStateOf<WebDAVResource?>(null) }
    // 重命名对话框
    var renameTarget by remember { mutableStateOf<WebDAVResource?>(null) }
    // 移动对话框
    var moveTarget by remember { mutableStateOf<WebDAVResource?>(null) }
    // 删除确认对话框
    var deleteTarget by remember { mutableStateOf<WebDAVResource?>(null) }
    
    // 操作成功/失败提示
    LaunchedEffect(uiState.operationSuccess) {
        uiState.operationSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearOperationFeedback()
        }
    }
    LaunchedEffect(uiState.operationError) {
        uiState.operationError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearOperationFeedback()
        }
    }
    
    // 初始化服务器连接
    LaunchedEffect(serverId) {
        serverId?.let { viewModel.selectServerById(it) }
    }
    
    // 文件列表变化时加载收藏状态和下载状态
    LaunchedEffect(uiState.files) {
        if (uiState.files.isNotEmpty()) {
            viewModel.loadFavoriteStates(uiState.files.map { it.path })
            viewModel.loadDownloadStates(uiState.files.map { it.path })
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("文件浏览器")
                        if (currentPath.isNotEmpty() && currentPath != "/") {
                            Text(
                                text = currentPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.refresh() }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 面包屑导航
            if (uiState.isConnected && currentPath.isNotEmpty()) {
                Breadcrumb(
                    path = currentPath,
                    onNavigate = { path -> viewModel.navigateTo(path) }
                )
                HorizontalDivider()
            }
            
            // 内容区域
            when {
                !uiState.isConnected -> {
                    // 未连接状态
                    NotConnectedState(
                        onRetry = { serverId?.let { viewModel.selectServerById(it) } }
                    )
                }
                uiState.isLoading -> {
                    // 加载中
                    LoadingState()
                }
                uiState.error != null -> {
                    // 错误状态
                    ErrorState(
                        error = uiState.error ?: "未知错误",
                        onRetry = { viewModel.refresh() }
                    )
                }
                uiState.files.isEmpty() -> {
                    // 空目录
                    EmptyDirectoryState()
                }
                else -> {
                    // 文件列表
                    FileList(
                        files = uiState.files,
                        videoPreviews = videoPreviews,
                        favoriteStates = favoriteStates,
                        downloadStates = downloadStates,
                        onFileClick = { resource ->
                            handleFileClick(
                                resource = resource,
                                viewModel = viewModel,
                                onVideoClick = onVideoClick,
                                onImageClick = onImageClick
                            )
                        },
                        onPreviewClick = { images, index ->
                            previewState = PreviewState(images, index)
                        },
                        onLoadPreviews = { path ->
                            viewModel.loadVideoPreviews(path)
                        },
                        onToggleFavorite = { resource ->
                            viewModel.toggleFavorite(resource)
                        },
                        onDownloadClick = { resource ->
                            viewModel.startDownload(resource)
                        },
                        onRetryDownload = { resource ->
                            viewModel.retryDownload(resource.path)
                        },
                        onCancelDownload = { resource ->
                            viewModel.cancelDownload(resource.path)
                        },
                        onMoreClick = { resource ->
                            menuResource = resource
                        }
                    )
                }
            }
        }
    }
    
    // 全屏预览图对话框
    previewState?.let { state ->
        ImagePreviewDialog(
            images = state.images,
            initialIndex = state.initialIndex,
            onDismiss = { previewState = null }
        )
    }
    
    // 更多操作菜单
    menuResource?.let { resource ->
        OperationMenu(
            onDismiss = { menuResource = null },
            onRename = {
                menuResource = null
                renameTarget = resource
            },
            onMove = {
                menuResource = null
                moveTarget = resource
            },
            onDelete = {
                menuResource = null
                deleteTarget = resource
            }
        )
    }
    
    // 重命名对话框
    renameTarget?.let { resource ->
        RenameDialog(
            resource = resource,
            isLoading = uiState.isOperationLoading,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                viewModel.renameResource(resource, newName)
                renameTarget = null
            }
        )
    }
    
    // 移动对话框
    moveTarget?.let { resource ->
        MoveDialog(
            resource = resource,
            currentPath = currentPath,
            isLoading = uiState.isOperationLoading,
            loadDirectories = { path ->
                viewModel.listDirectories(path)
            },
            onDismiss = { moveTarget = null },
            onConfirm = { destinationDir ->
                viewModel.moveResource(resource, destinationDir)
                moveTarget = null
            }
        )
    }
    
    // 删除确认对话框
    deleteTarget?.let { resource ->
        DeleteConfirmDialog(
            resource = resource,
            isLoading = uiState.isOperationLoading,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.deleteResource(resource)
                deleteTarget = null
            }
        )
    }
}

/**
 * 预览状态
 */
private data class PreviewState(
    val images: List<String>,
    val initialIndex: Int
)

/**
 * 文件列表
 */
@Composable
private fun FileList(
    files: List<WebDAVResource>,
    videoPreviews: Map<String, List<String>>,
    favoriteStates: Map<String, Boolean>,
    downloadStates: Map<String, DownloadState>,
    onFileClick: (WebDAVResource) -> Unit,
    onPreviewClick: (List<String>, Int) -> Unit,
    onLoadPreviews: (String) -> Unit,
    onToggleFavorite: (WebDAVResource) -> Unit,
    onDownloadClick: (WebDAVResource) -> Unit,
    onRetryDownload: (WebDAVResource) -> Unit,
    onCancelDownload: (WebDAVResource) -> Unit,
    onMoreClick: (WebDAVResource) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = files,
            key = { it.path }
        ) { resource ->
            // 加载视频预览图
            val previews = if (resource.isVideo) {
                videoPreviews[resource.path] ?: emptyList()
            } else {
                emptyList()
            }
            
            FileItem(
                resource = resource,
                onClick = { onFileClick(resource) },
                previewImages = previews,
                onPreviewClick = onPreviewClick,
                onLoadPreviews = { onLoadPreviews(resource.path) },
                isFavorite = favoriteStates[resource.path] ?: false,
                onFavoriteClick = { onToggleFavorite(resource) },
                downloadState = downloadStates[resource.path] ?: DownloadState.NotDownloaded,
                onDownloadClick = { onDownloadClick(resource) },
                onRetryClick = { onRetryDownload(resource) },
                onCancelDownload = { onCancelDownload(resource) },
                onMoreClick = { onMoreClick(resource) }
            )
        }
    }
}

/**
 * 加载状态
 */
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * 未连接状态
 */
@Composable
private fun NotConnectedState(
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "未连接到服务器",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "请先在设置中选择并连接服务器",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

/**
 * 错误状态
 */
@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "加载失败",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

/**
 * 空目录状态
 */
@Composable
private fun EmptyDirectoryState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "空目录",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "当前目录没有文件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * 处理文件点击事件
 */
private fun handleFileClick(
    resource: WebDAVResource,
    viewModel: FileBrowserViewModel,
    onVideoClick: (String) -> Unit,
    onImageClick: (String) -> Unit
) {
    // 日志打印资源URL
    Log.d("FileBrowserScreen", "File clicked: ${resource.path}")

    when {
        resource.isDirectory -> {
            // 进入目录
            viewModel.navigateTo(resource.path)
        }
        resource.isVideo -> {
            // 播放视频
            val streamUrl = viewModel.getStreamUrl(resource.path)
            Log.d("FileBrowserScreen", "Video clicked: ${streamUrl}")
            onVideoClick(streamUrl)
        }
        resource.isImage -> {
            // 查看图片
            val streamUrl = viewModel.getStreamUrl(resource.path)
            Log.d("FileBrowserScreen", "Image clicked: ${streamUrl}")
            onImageClick(streamUrl)
        }
        else -> {
            // 其他类型文件，暂不处理
        }
    }
}

/**
 * 更多操作菜单
 */
@Composable
private fun OperationMenu(
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("重命名") },
            leadingIcon = { Icon(Icons.Default.Create, contentDescription = null) },
            onClick = onRename
        )
        DropdownMenuItem(
            text = { Text("移动") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null) },
            onClick = onMove
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = "删除",
                    color = MaterialTheme.colorScheme.error
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            onClick = onDelete
        )
    }
}

/**
 * 重命名对话框
 */
@Composable
private fun RenameDialog(
    resource: WebDAVResource,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(TextFieldValue(resource.name)) }
    var nameError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("重命名") },
        text = {
            Column {
                Text(
                    text = "当前名称：${resource.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        nameError = null
                    },
                    label = { Text("新名称") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = newName.text.trim()
                    if (trimmed.isEmpty()) {
                        nameError = "名称不能为空"
                    } else if (trimmed == resource.name) {
                        nameError = "名称未发生变化"
                    } else if (trimmed.contains('/') || trimmed.contains('\\')) {
                        nameError = "名称不能包含路径分隔符"
                    } else {
                        onConfirm(trimmed)
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("确认")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("取消")
            }
        }
    )
}

/**
 * 移动对话框
 * 通过浏览当前服务器目录选择目标位置
 */
@Composable
private fun MoveDialog(
    resource: WebDAVResource,
    currentPath: String,
    isLoading: Boolean,
    loadDirectories: suspend (String) -> List<WebDAVResource>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // 记录当前浏览的目录（用于选择目标位置），从资源父目录开始
    var browsePath by remember(resource.path) { mutableStateOf(getParentPath(resource.path)) }
    // 当前选中/展示的目标目录
    val selectedDir = browsePath
    
    // 目标目录下的子目录列表（用于进入更深层目录）
    var subDirectories by remember { mutableStateOf<List<WebDAVResource>>(emptyList()) }
    // 目录加载中状态
    var loadingDirs by remember { mutableStateOf(false) }
    
    // 浏览路径变化时，加载该目录下的子目录
    LaunchedEffect(browsePath) {
        loadingDirs = true
        subDirectories = loadDirectories(browsePath)
        loadingDirs = false
    }
    
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("移动 \"${resource.name}\"") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Text(
                    text = "选择目标目录：$selectedDir",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // 常用位置：根目录、当前目录
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { browsePath = "/" },
                        enabled = !isLoading
                    ) {
                        Text("根目录")
                    }
                    OutlinedButton(
                        onClick = { browsePath = getParentPath(currentPath) },
                        enabled = !isLoading
                    ) {
                        Text("当前目录")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // 向上返回按钮
                if (selectedDir != "/") {
                    TextButton(
                        onClick = { browsePath = getParentPath(selectedDir) },
                        enabled = !isLoading
                    ) {
                        Text("↑ 上一级")
                    }
                    HorizontalDivider()
                }
                
                // 子目录列表
                if (loadingDirs) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (subDirectories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "此目录下没有子目录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(subDirectories, key = { it.path }) { dir ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        enabled = !isLoading,
                                        onClick = { browsePath = dir.path }
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dir.name)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "进入",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedDir) },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("移动到此处")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("取消")
            }
        }
    )
}

/**
 * 删除确认对话框
 */
@Composable
private fun DeleteConfirmDialog(
    resource: WebDAVResource,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("删除确认") },
        text = {
            Text(
                text = if (resource.isDirectory) {
                    "确定要删除文件夹 \"${resource.name}\" 吗？该操作将递归删除文件夹内所有内容，且无法恢复！"
                } else {
                    "确定要删除文件 \"${resource.name}\" 吗？该操作无法恢复！"
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("删除")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("取消")
            }
        }
    )
}

/**
 * 获取父目录路径
 * "/movies/aaa.mp4" -> "/movies/"
 * "aaa.mp4" -> "/"
 */
private fun getParentPath(path: String): String {
    val normalizedPath = path.trimStart('/').trimEnd('/')
    if (normalizedPath.isEmpty()) return "/"
    val lastSlashIndex = normalizedPath.lastIndexOf('/')
    return if (lastSlashIndex < 0) {
        "/"
    } else {
        "/${normalizedPath.substring(0, lastSlashIndex)}/"
    }
}
