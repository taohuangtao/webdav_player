package com.tdull.webdavviewer.app.ui.browser

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.tdull.webdavviewer.app.data.model.BrowserLayoutMode
import com.tdull.webdavviewer.app.data.model.DownloadState
import com.tdull.webdavviewer.app.data.model.ResourceType
import com.tdull.webdavviewer.app.data.model.UploadConflictPolicy
import com.tdull.webdavviewer.app.data.model.WebDAVResource
import com.tdull.webdavviewer.app.ui.components.MenuItemRow
import com.tdull.webdavviewer.app.ui.components.MenuPopupContainer
import com.tdull.webdavviewer.app.ui.viewer.ImageViewerItem
import com.tdull.webdavviewer.app.viewmodel.FileBrowserViewModel
import com.tdull.webdavviewer.app.viewmodel.PendingUploadBatch

// ================= 文件浏览器设计稿配色（filebrowser_redesign.html） =================
private val SettingsBg = Color(0xFFF4F6FB)      // 页面背景
private val CardWhite = Color(0xFFFFFFFF)        // 卡片底色
private val TextPrimary = Color(0xFF111827)      // 主文字
private val TextSecondary = Color(0xFF6B7280)    // 次级文字
private val TextMuted = Color(0xFF9CA3AF)        // 弱化文字
private val IndigoPrimary = Color(0xFF4F46E5)    // indigo 主色
private val IndigoLight = Color(0xFFEEF2FF)      // indigo 浅底
private val DeleteRed = Color(0xFFF43F5E)        // 删除红
private val DividerColor = Color(0xFFF3F4F6)     // 分割线

/**
 * 文件浏览器页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    viewModel: FileBrowserViewModel = hiltViewModel(),
    serverId: String? = null,
    onVideoClick: (String) -> Unit = {},
    onImageClick: (List<ImageViewerItem>, Int) -> Unit = { _, _ -> },
    onNavigateToUploads: () -> Unit = {},
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
    
    // 重命名对话框
    var renameTarget by remember { mutableStateOf<WebDAVResource?>(null) }
    // 移动对话框
    var moveTarget by remember { mutableStateOf<WebDAVResource?>(null) }
    // 删除确认对话框
    var deleteTarget by remember { mutableStateOf<WebDAVResource?>(null) }
    // 新建文件夹对话框
    var showCreateDirectoryDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            viewModel.prepareUploads(uris)
        }
    )

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
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
        containerColor = SettingsBg,
        topBar = {
            // 紧凑顶部导航栏（设计稿风格：浅底 + 深色标题 + indigo 眼睛按钮）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SettingsBg)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = "文件浏览器",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    LayoutModeButton(
                        layoutMode = uiState.layoutMode,
                        onClick = { viewModel.toggleLayoutMode() }
                    )
                    if (uiState.layoutMode == BrowserLayoutMode.GRID) {
                        Spacer(modifier = Modifier.width(6.dp))
                        GridColumnsControl(
                            columns = uiState.gridColumns,
                            onDecrease = { viewModel.decreaseGridColumns() },
                            onIncrease = { viewModel.increaseGridColumns() }
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    BrowserMoreMenu(
                        canWrite = uiState.isConnected && !uiState.isOperationLoading && !uiState.isPreparingUpload,
                        showHidden = uiState.showHidden,
                        onRefresh = { viewModel.refresh() },
                        onCreateDirectory = { showCreateDirectoryDialog = true },
                        onUploadFiles = {
                            requestNotificationPermissionIfNeeded()
                            filePickerLauncher.launch(arrayOf("*/*"))
                        },
                        onNavigateToUploads = onNavigateToUploads,
                        onToggleShowHidden = { viewModel.toggleShowHidden() }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isPreparingUpload) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = IndigoPrimary,
                    trackColor = IndigoLight
                )
            }
            // 面包屑导航
            if (uiState.isConnected && currentPath.isNotEmpty()) {
                Breadcrumb(
                    path = currentPath,
                    onNavigate = { path -> viewModel.navigateTo(path) }
                )
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
                        layoutMode = uiState.layoutMode,
                        gridColumns = uiState.gridColumns,
                        videoPreviews = videoPreviews,
                        favoriteStates = favoriteStates,
                        downloadStates = downloadStates,
                        onFileClick = { resource ->
                            handleFileClick(
                                resource = resource,
                                files = uiState.files,
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
                        getImageThumbnailUrl = { path ->
                            viewModel.getImageThumbnailUrl(path)
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
                        onRename = { resource ->
                            renameTarget = resource
                        },
                        onMove = { resource ->
                            moveTarget = resource
                        },
                        onDelete = { resource ->
                            deleteTarget = resource
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

    if (showCreateDirectoryDialog) {
        CreateDirectoryDialog(
            currentPath = currentPath,
            isLoading = uiState.isOperationLoading,
            onDismiss = { showCreateDirectoryDialog = false },
            onConfirm = { folderName ->
                viewModel.createDirectory(folderName)
                showCreateDirectoryDialog = false
            }
        )
    }

    uiState.pendingUploadBatch?.let { batch ->
        UploadConfirmDialog(
            batch = batch,
            isLoading = uiState.isOperationLoading,
            onDismiss = { viewModel.dismissPendingUploads() },
            onSkipConflicts = { viewModel.enqueuePendingUploads(UploadConflictPolicy.SKIP) },
            onOverwriteConflicts = { viewModel.enqueuePendingUploads(UploadConflictPolicy.OVERWRITE) }
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

@Composable
private fun BrowserMoreMenu(
    canWrite: Boolean,
    showHidden: Boolean,
    onRefresh: () -> Unit,
    onCreateDirectory: () -> Unit,
    onUploadFiles: () -> Unit,
    onNavigateToUploads: () -> Unit,
    onToggleShowHidden: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(IndigoLight, RoundedCornerShape(18.dp))
                .clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "更多操作",
                tint = IndigoPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        MenuPopupContainer(
            expanded = expanded,
            onDismiss = { expanded = false }
        ) {
            MenuItemRow(
                icon = Icons.Default.Refresh,
                iconTint = IndigoPrimary,
                text = "刷新",
                textColor = TextPrimary,
                onClick = {
                    expanded = false
                    onRefresh()
                }
            )
            MenuItemRow(
                icon = Icons.Default.CreateNewFolder,
                iconTint = IndigoPrimary,
                text = "新建文件夹",
                textColor = TextPrimary,
                enabled = canWrite,
                onClick = {
                    expanded = false
                    onCreateDirectory()
                }
            )
            MenuItemRow(
                icon = Icons.Default.UploadFile,
                iconTint = IndigoPrimary,
                text = "上传文件",
                textColor = TextPrimary,
                enabled = canWrite,
                onClick = {
                    expanded = false
                    onUploadFiles()
                }
            )
            MenuItemRow(
                icon = Icons.AutoMirrored.Filled.List,
                iconTint = IndigoPrimary,
                text = "上传任务",
                textColor = TextPrimary,
                onClick = {
                    expanded = false
                    onNavigateToUploads()
                }
            )
            MenuItemRow(
                icon = if (showHidden) Icons.Default.Visibility else Icons.Outlined.VisibilityOff,
                iconTint = IndigoPrimary,
                text = if (showHidden) "隐藏隐藏文件" else "显示隐藏文件",
                textColor = TextPrimary,
                onClick = {
                    expanded = false
                    onToggleShowHidden()
                }
            )
        }
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
    layoutMode: BrowserLayoutMode,
    gridColumns: Int,
    videoPreviews: Map<String, List<String>>,
    favoriteStates: Map<String, Boolean>,
    downloadStates: Map<String, DownloadState>,
    onFileClick: (WebDAVResource) -> Unit,
    onPreviewClick: (List<String>, Int) -> Unit,
    onLoadPreviews: (String) -> Unit,
    getImageThumbnailUrl: (String) -> String,
    onToggleFavorite: (WebDAVResource) -> Unit,
    onDownloadClick: (WebDAVResource) -> Unit,
    onRetryDownload: (WebDAVResource) -> Unit,
    onCancelDownload: (WebDAVResource) -> Unit,
    onRename: (WebDAVResource) -> Unit,
    onMove: (WebDAVResource) -> Unit,
    onDelete: (WebDAVResource) -> Unit
) {
    if (layoutMode == BrowserLayoutMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(
                count = files.size,
                key = { index -> files[index].path }
            ) { index ->
                val resource = files[index]
                GridFileItem(
                    resource = resource,
                    thumbnailUrl = if (resource.isImage) {
                        getImageThumbnailUrl(resource.path)
                    } else {
                        null
                    },
                    onClick = { onFileClick(resource) },
                    moreMenuContent = { onDismiss ->
                        FileMenuItems(
                            resource = resource,
                            downloadState = downloadStates[resource.path] ?: DownloadState.NotDownloaded,
                            isFavorite = favoriteStates[resource.path] ?: false,
                            onDismiss = onDismiss,
                            onEnter = { onFileClick(resource) },
                            onDownload = { onDownloadClick(resource) },
                            onRetryDownload = { onRetryDownload(resource) },
                            onToggleFavorite = { onToggleFavorite(resource) },
                            onRename = { onRename(resource) },
                            onMove = { onMove(resource) },
                            onDelete = { onDelete(resource) }
                        )
                    }
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(CardWhite),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            itemsIndexed(
                items = files,
                key = { _, item -> item.path }
            ) { index, resource ->
                // 加载视频预览图
                val previews = if (resource.isVideo) {
                    videoPreviews[resource.path] ?: emptyList()
                } else {
                    emptyList()
                }

                FileItem(
                    resource = resource,
                    onClick = { onFileClick(resource) },
                    thumbnailUrl = if (resource.isImage) {
                        getImageThumbnailUrl(resource.path)
                    } else {
                        null
                    },
                    previewImages = previews,
                    onPreviewClick = onPreviewClick,
                    onLoadPreviews = { onLoadPreviews(resource.path) },
                    downloadState = downloadStates[resource.path] ?: DownloadState.NotDownloaded,
                    onCancelDownload = { onCancelDownload(resource) },
                    moreMenuContent = { onDismiss ->
                        FileMenuItems(
                            resource = resource,
                            downloadState = downloadStates[resource.path] ?: DownloadState.NotDownloaded,
                            isFavorite = favoriteStates[resource.path] ?: false,
                            onDismiss = onDismiss,
                            onEnter = { onFileClick(resource) },
                            onDownload = { onDownloadClick(resource) },
                            onRetryDownload = { onRetryDownload(resource) },
                            onToggleFavorite = { onToggleFavorite(resource) },
                            onRename = { onRename(resource) },
                            onMove = { onMove(resource) },
                            onDelete = { onDelete(resource) }
                        )
                    }
                )
                // 行间分割线（最后一行不加，增强视觉分隔）
                if (index < files.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 62.dp),
                        color = DividerColor
                    )
                }
            }
        }
    }
}

@Composable
private fun LayoutModeButton(
    layoutMode: BrowserLayoutMode,
    onClick: () -> Unit
) {
    val isGridMode = layoutMode == BrowserLayoutMode.GRID
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(IndigoLight, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isGridMode) Icons.AutoMirrored.Filled.List else Icons.Default.Apps,
            contentDescription = if (isGridMode) "切换为列表模式" else "切换为网格模式",
            tint = IndigoPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun GridColumnsControl(
    columns: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .background(IndigoLight, RoundedCornerShape(18.dp))
            .padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GridColumnButton(
            icon = Icons.Default.Remove,
            contentDescription = "减少列数",
            enabled = columns > 2,
            onClick = onDecrease
        )
        Text(
            text = columns.toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = IndigoPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(18.dp)
        )
        GridColumnButton(
            icon = Icons.Default.Add,
            contentDescription = "增加列数",
            enabled = columns < 8,
            onClick = onIncrease
        )
    }
}

@Composable
private fun GridColumnButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) IndigoPrimary else TextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GridFileItem(
    resource: WebDAVResource,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    moreMenuContent: (@Composable (onDismiss: () -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (moreMenuContent != null) {
                        showMenu = true
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        GridResourceVisual(
            resource = resource,
            thumbnailUrl = thumbnailUrl,
            modifier = Modifier.matchParentSize()
        )

        if (showMenu && moreMenuContent != null) {
            MenuPopupContainer(
                expanded = showMenu,
                onDismiss = { showMenu = false }
            ) {
                moreMenuContent { showMenu = false }
            }
        }
    }
}

@Composable
private fun GridResourceVisual(
    resource: WebDAVResource,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier
) {
    val icon = getGridResourceIcon(resource.resourceType)
    val iconColor = getGridResourceIconColor(resource.resourceType)
    val iconBg = getGridResourceIconBg(resource.resourceType)

    if (resource.isImage && thumbnailUrl != null) {
        var thumbnailLoaded by remember(thumbnailUrl) { mutableStateOf(false) }

        Box(
            modifier = modifier.background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            if (!thumbnailLoaded) {
                GridDefaultIcon(
                    resourceName = resource.name,
                    icon = icon,
                    contentDescription = getGridResourceTypeName(resource.resourceType),
                    tint = iconColor
                )
            }
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "图片缩略图",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                onLoading = { thumbnailLoaded = false },
                onSuccess = { thumbnailLoaded = true },
                onError = { thumbnailLoaded = false }
            )
        }
    } else {
        Box(
            modifier = modifier.background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            GridDefaultIcon(
                resourceName = resource.name,
                icon = icon,
                contentDescription = getGridResourceTypeName(resource.resourceType),
                tint = iconColor
            )
        }
    }
}

@Composable
private fun GridDefaultIcon(
    resourceName: String,
    icon: ImageVector,
    contentDescription: String,
    tint: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .fillMaxSize(0.34f)
                .align(Alignment.Center)
                .offset(y = (-9).dp)
        )
        Text(
            text = resourceName,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }
}

private fun getGridResourceIcon(type: ResourceType): ImageVector {
    return when (type) {
        ResourceType.DIRECTORY -> Icons.Default.Folder
        ResourceType.VIDEO -> Icons.Default.PlayArrow
        ResourceType.IMAGE -> Icons.Default.Image
        ResourceType.AUDIO -> Icons.Default.Phone
        ResourceType.OTHER -> Icons.Default.Info
    }
}

private fun getGridResourceIconColor(type: ResourceType): Color {
    return when (type) {
        ResourceType.DIRECTORY -> IndigoPrimary
        ResourceType.VIDEO -> DeleteRed
        ResourceType.IMAGE -> Color(0xFF0EA5E9)
        ResourceType.AUDIO -> IndigoPrimary
        ResourceType.OTHER -> Color(0xFFF59E0B)
    }
}

private fun getGridResourceIconBg(type: ResourceType): Color {
    return when (type) {
        ResourceType.DIRECTORY -> IndigoLight
        ResourceType.VIDEO -> Color(0xFFFFF1F2)
        ResourceType.IMAGE -> Color(0xFFF0F9FF)
        ResourceType.AUDIO -> IndigoLight
        ResourceType.OTHER -> Color(0xFFFFFBEB)
    }
}

private fun getGridResourceTypeName(type: ResourceType): String {
    return when (type) {
        ResourceType.DIRECTORY -> "文件夹"
        ResourceType.VIDEO -> "视频"
        ResourceType.IMAGE -> "图片"
        ResourceType.AUDIO -> "音频"
        ResourceType.OTHER -> "文件"
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
                tint = TextMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "未连接到服务器",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "请先在设置中选择并连接服务器",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
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
                color = TextMuted,
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
                tint = TextMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "空目录",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "当前目录没有文件",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }
}

/**
 * 处理文件点击事件
 */
private fun handleFileClick(
    resource: WebDAVResource,
    files: List<WebDAVResource>,
    viewModel: FileBrowserViewModel,
    onVideoClick: (String) -> Unit,
    onImageClick: (List<ImageViewerItem>, Int) -> Unit
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
            val imageResources = files.filter { it.isImage }
            val imageItems = imageResources.map { imageResource ->
                ImageViewerItem(
                    url = viewModel.getStreamUrl(imageResource.path),
                    mediumThumbnailUrl = viewModel.getImageMediumThumbnailUrl(imageResource.path),
                    title = imageResource.name
                )
            }
            val initialIndex = imageResources
                .indexOfFirst { it.path == resource.path }
                .takeIf { it >= 0 }
                ?: 0
            val streamUrl = imageItems.getOrNull(initialIndex)?.url ?: viewModel.getStreamUrl(resource.path)
            Log.d("FileBrowserScreen", "Image clicked: ${streamUrl}")
            onImageClick(
                imageItems.ifEmpty {
                    listOf(
                        ImageViewerItem(
                            url = viewModel.getStreamUrl(resource.path),
                            mediumThumbnailUrl = viewModel.getImageMediumThumbnailUrl(resource.path),
                            title = resource.name
                        )
                    )
                },
                initialIndex
            )
        }
        else -> {
            // 其他类型文件，暂不处理
        }
    }
}

/**
 * 更多操作菜单项（在 FileItem 的 Popup 菜单容器内渲染，锚定到按钮位置）
 * 样式与 menu_redesign.html 设计稿一致：白底 16dp 圆角容器 + 44dp 高菜单项 + indigo 图标 / 红色删除
 */
@Composable
private fun OperationMenuItems(
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    MenuItemRow(
        icon = Icons.Default.Create,
        iconTint = IndigoPrimary,
        text = "重命名",
        textColor = TextPrimary,
        onClick = {
            onDismiss()
            onRename()
        }
    )
    MenuItemRow(
        icon = Icons.AutoMirrored.Filled.DriveFileMove,
        iconTint = IndigoPrimary,
        text = "移动",
        textColor = TextPrimary,
        onClick = {
            onDismiss()
            onMove()
        }
    )
    MenuItemRow(
        icon = Icons.Default.Delete,
        iconTint = DeleteRed,
        text = "删除",
        textColor = DeleteRed,
        onClick = {
            onDismiss()
            onDelete()
        }
    )
}

/**
 * 文件/目录的"更多操作"菜单项（在 FileItem 的 Popup 菜单容器内渲染）
 * 文件菜单：下载(仅视频,状态相关) / 收藏或取消收藏 / 重命名 / 移动 / 删除
 * 目录菜单：重命名 / 移动 / 删除（进入通过点击 item 本身实现）
 * 下载动作仅在非下载中状态显示（下载中由行内进度指示承担）
 */
@Composable
private fun FileMenuItems(
    resource: WebDAVResource,
    downloadState: DownloadState,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onEnter: () -> Unit,
    onDownload: () -> Unit,
    onRetryDownload: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    if (!resource.isDirectory) {
        // 文件菜单：下载(仅视频,非下载中) / 收藏或取消收藏
        if (resource.isVideo && downloadState !is DownloadState.Downloading) {
            MenuItemRow(
                icon = when (downloadState) {
                    is DownloadState.Downloaded -> Icons.Default.Check
                    is DownloadState.Error -> Icons.Default.Refresh
                    else -> Icons.Default.Download
                },
                iconTint = if (downloadState is DownloadState.Error) DeleteRed else IndigoPrimary,
                text = when (downloadState) {
                    is DownloadState.NotDownloaded -> "下载"
                    is DownloadState.Downloading -> ""
                    is DownloadState.Downloaded -> "打开"
                    is DownloadState.Error -> "重试"
                },
                textColor = if (downloadState is DownloadState.Error) DeleteRed else TextPrimary,
                onClick = {
                    onDismiss()
                    when (downloadState) {
                        is DownloadState.Downloaded -> onEnter()          // 已下载 → 打开播放
                        is DownloadState.Error -> onRetryDownload()       // 失败 → 重试下载
                        else -> onDownload()                              // 未下载 → 下载
                    }
                }
            )
        }
        MenuItemRow(
            icon = if (isFavorite) Icons.Default.Star else Icons.Outlined.Star,
            iconTint = if (isFavorite) IndigoPrimary else TextSecondary,
            text = if (isFavorite) "取消收藏" else "收藏",
            textColor = TextPrimary,
            onClick = {
                onDismiss()
                onToggleFavorite()
            }
        )
    }
    // 重命名 / 移动 / 删除（文件与目录通用）
    OperationMenuItems(
        onDismiss = onDismiss,
        onRename = onRename,
        onMove = onMove,
        onDelete = onDelete
    )
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
 * 新建文件夹对话框
 */
@Composable
private fun CreateDirectoryDialog(
    currentPath: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var folderName by remember { mutableStateOf(TextFieldValue("")) }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("新建文件夹") },
        text = {
            Column {
                Text(
                    text = "当前位置：$currentPath",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = {
                        folderName = it
                        nameError = null
                    },
                    label = { Text("文件夹名称") },
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
                    val trimmed = folderName.text.trim()
                    if (trimmed.isEmpty()) {
                        nameError = "名称不能为空"
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
                    Text("创建")
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
 * 上传确认对话框。
 */
@Composable
private fun UploadConfirmDialog(
    batch: PendingUploadBatch,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSkipConflicts: () -> Unit,
    onOverwriteConflicts: () -> Unit
) {
    val hasConflicts = batch.conflictFileNames.isNotEmpty()
    val uploadableFiles = batch.files.filterNot { it.fileName in batch.blockedFileNames }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("上传文件") },
        text = {
            Column {
                Text(
                    text = "目标位置：${batch.serverConfig.name}${batch.targetDirectory}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "将上传 ${uploadableFiles.size} 个文件",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                UploadNamePreview(names = uploadableFiles.map { it.fileName })

                if (batch.duplicateFileNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "同批次重名文件只保留第一个：${batch.duplicateFileNames.take(3).joinToString("、")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                if (batch.blockedFileNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "以下文件与远端文件夹重名，无法上传：${batch.blockedFileNames.take(3).joinToString("、")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeleteRed
                    )
                }

                if (hasConflicts) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "发现 ${batch.conflictFileNames.size} 个远端同名文件",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeleteRed,
                        fontWeight = FontWeight.SemiBold
                    )
                    UploadNamePreview(names = batch.conflictFileNames.toList())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = if (hasConflicts) onSkipConflicts else onSkipConflicts,
                enabled = !isLoading && uploadableFiles.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(if (hasConflicts) "跳过同名" else "上传")
                }
            }
        },
        dismissButton = {
            Row {
                if (hasConflicts) {
                    TextButton(
                        onClick = onOverwriteConflicts,
                        enabled = !isLoading
                    ) {
                        Text("覆盖同名")
                    }
                }
                TextButton(onClick = onDismiss, enabled = !isLoading) {
                    Text("取消")
                }
            }
        }
    )
}

@Composable
private fun UploadNamePreview(names: List<String>) {
    if (names.isEmpty()) return
    Spacer(modifier = Modifier.height(6.dp))
    Column {
        names.take(5).forEach { name ->
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (names.size > 5) {
            Text(
                text = "还有 ${names.size - 5} 个文件",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
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
