package com.tdull.webdavviewer.app.ui.browser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
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
    
    // 全屏预览图状态
    var previewState by remember { mutableStateOf<PreviewState?>(null) }
    
    // 初始化服务器连接
    LaunchedEffect(serverId) {
        serverId?.let { viewModel.selectServerById(it) }
    }
    
    // 文件列表变化时加载收藏状态
    LaunchedEffect(uiState.files) {
        if (uiState.files.isNotEmpty()) {
            viewModel.loadFavoriteStates(uiState.files.map { it.path })
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
    onFileClick: (WebDAVResource) -> Unit,
    onPreviewClick: (List<String>, Int) -> Unit,
    onLoadPreviews: (String) -> Unit,
    onToggleFavorite: (WebDAVResource) -> Unit
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
                onFavoriteClick = { onToggleFavorite(resource) }
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
