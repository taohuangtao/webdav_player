package com.tdull.webdavviewer.app.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tdull.webdavviewer.app.data.model.DownloadItem
import com.tdull.webdavviewer.app.data.model.DownloadState
import com.tdull.webdavviewer.app.service.DownloadProgress
import com.tdull.webdavviewer.app.ui.browser.FileItem
import com.tdull.webdavviewer.app.ui.components.MenuItemRow
import com.tdull.webdavviewer.app.viewmodel.DownloadsViewModel

// ================= 下载页设计稿配色（与 filebrowser_redesign.html 统一） =================
private val SettingsBg = Color(0xFFF4F6FB)      // 页面背景
private val CardWhite = Color(0xFFFFFFFF)        // 卡片底色
private val TextPrimary = Color(0xFF111827)      // 主文字
private val TextSecondary = Color(0xFF6B7280)    // 次级文字
private val TextMuted = Color(0xFF9CA3AF)        // 弱化文字
private val IndigoPrimary = Color(0xFF4F46E5)    // indigo 主色
private val IndigoLight = Color(0xFFEEF2FF)      // indigo 浅底
private val ErrorPrimary = Color(0xFFF43F5E)     // 失败红
private val ErrorLight = Color(0xFFFFF1F2)       // 失败浅底
private val DividerColor = Color(0xFFF3F4F6)     // 分割线

/**
 * 下载列表页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
    onVideoClick: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = SettingsBg,
        topBar = {
            // 紧凑顶部导航栏（与文件浏览器统一）
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
                        text = "下载管理",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
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
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                activeDownloads.isEmpty() && downloads.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    DownloadList(
                        activeDownloads = activeDownloads,
                        downloads = downloads,
                        isFileExists = { path -> viewModel.isFileExists(path) },
                        onDownloadClick = { download ->
                            handleDownloadClick(
                                download = download,
                                viewModel = viewModel,
                                onVideoClick = onVideoClick
                            )
                        },
                        onDeleteClick = { download ->
                            viewModel.showDeleteConfirm(download)
                        },
                        onRetryDownload = { resourcePath ->
                            viewModel.retryDownload(resourcePath)
                        },
                        onCancelDownload = { resourcePath ->
                            viewModel.cancelDownload(resourcePath)
                        }
                    )
                }
            }
        }
    }

    // 删除确认对话框
    if (uiState.showDeleteConfirm && uiState.itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            title = { Text("确认删除") },
            text = {
                Column {
                    Text("确定要删除这个下载吗？")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.itemToDelete!!.videoTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "这将删除本地文件，无法恢复",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteDownload() }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirm() }) {
                    Text("取消")
                }
            }
        )
    }

    // 错误提示
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // 可以显示 Snackbar
        }
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("错误") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("确定")
                }
            }
        )
    }
}

/**
 * 下载列表（包含下载中和已下载，白卡片包裹，与文件浏览器一致）
 */
@Composable
private fun DownloadList(
    activeDownloads: Map<String, DownloadProgress>,
    downloads: List<DownloadItem>,
    isFileExists: (String) -> Boolean,
    onDownloadClick: (DownloadItem) -> Unit,
    onDeleteClick: (DownloadItem) -> Unit,
    onRetryDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(CardWhite),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        // 下载中的项
        if (activeDownloads.isNotEmpty()) {
            item(key = "downloading_header") {
                Text(
                    text = "下载中",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            items(
                items = activeDownloads.entries.toList(),
                key = { it.key }
            ) { entry ->
                ActiveDownloadCard(
                    progress = entry.value,
                    onRetry = { onRetryDownload(entry.key) },
                    onCancel = { onCancelDownload(entry.key) }
                )
            }
        }

        // 已下载的项
        if (downloads.isNotEmpty()) {
            // 分组间分割线（前面有"下载中"分组时）
            if (activeDownloads.isNotEmpty()) {
                item(key = "divider_before_downloaded") {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 62.dp),
                        color = DividerColor
                    )
                }
            }
            item(key = "downloaded_header") {
                Text(
                    text = "已下载",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            itemsIndexed(
                items = downloads,
                key = { _, item -> item.id }
            ) { index, download ->
                val fileExists = isFileExists(download.localPath)
                FileItem(
                    resource = download.toWebDAVResource(),
                    onClick = { onDownloadClick(download) },
                    downloadState = DownloadState.Downloaded,
                    fileMissing = !fileExists,
                    moreMenuContent = { onDismiss ->
                        MenuItemRow(
                            icon = Icons.Default.PlayArrow,
                            iconTint = TextPrimary,
                            text = "打开",
                            textColor = TextPrimary,
                            onClick = {
                                onDismiss()
                                onDownloadClick(download)
                            }
                        )
                        if (!fileExists) {
                            MenuItemRow(
                                icon = Icons.Default.Refresh,
                                iconTint = IndigoPrimary,
                                text = "重新下载",
                                textColor = TextPrimary,
                                onClick = {
                                    onDismiss()
                                    onRetryDownload(download.resourcePath)
                                }
                            )
                        }
                        MenuItemRow(
                            icon = Icons.Default.Delete,
                            iconTint = ErrorPrimary,
                            text = "删除",
                            textColor = ErrorPrimary,
                            onClick = {
                                onDismiss()
                                onDeleteClick(download)
                            }
                        )
                    }
                )
                // 行间分割线（最后一行不加）
                if (index < downloads.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 62.dp),
                        color = DividerColor
                    )
                }
            }
        }
    }
}

/**
 * 正在下载的卡片（行式，与文件浏览器 FileItem 风格统一）
 */
@Composable
private fun ActiveDownloadCard(
    progress: DownloadProgress,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标（彩色浅底方块，与 FileItem 一致）
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (progress.isFailed) ErrorLight else IndigoLight,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (progress.isFailed) Icons.Default.VideoFile else Icons.Default.Download,
                    contentDescription = if (progress.isFailed) "下载失败" else "下载中",
                    tint = if (progress.isFailed) ErrorPrimary else IndigoPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 文件信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = progress.fileName.ifEmpty { "未命名文件" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                if (progress.isFailed) {
                    Text(
                        text = progress.error ?: "下载失败",
                        fontSize = 12.sp,
                        color = ErrorPrimary
                    )
                } else {
                    Text(
                        text = "${progress.progressPercent}% · ${formatFileSize(progress.downloadedBytes)} / ${formatFileSize(progress.totalBytes)}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // 操作按钮
            if (progress.isFailed) {
                IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重试",
                        tint = IndigoPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = if (progress.isFailed) "关闭" else "取消",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 进度条（仅下载中显示）
        if (progress.isDownloading) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.progress },
                modifier = Modifier.fillMaxWidth(),
                color = IndigoPrimary,
                trackColor = IndigoLight
            )
        }
    }
}

/**
 * 处理下载项点击
 */
private fun handleDownloadClick(
    download: DownloadItem,
    viewModel: DownloadsViewModel,
    onVideoClick: (String) -> Unit
) {
    if (!viewModel.isFileExists(download.localPath)) {
        return
    }

    // 使用本地文件路径播放
    val localUrl = viewModel.getLocalVideoUrl(download.localPath)
    if (localUrl.isNotEmpty()) {
        onVideoClick(localUrl)
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
 * 空状态
 */
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TextMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无下载",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "在文件浏览器中点击下载按钮保存视频",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(size: Long): String {
    if (size < 0) return "未知"

    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var unitIndex = 0
    var fileSize = size.toDouble()

    while (fileSize >= 1024 && unitIndex < units.size - 1) {
        fileSize /= 1024
        unitIndex++
    }

    return String.format("%.1f %s", fileSize, units[unitIndex])
}
