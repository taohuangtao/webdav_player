package com.tdull.webdavviewer.app.ui.downloads

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tdull.webdavviewer.app.data.model.DownloadItem
import com.tdull.webdavviewer.app.viewmodel.DownloadsViewModel
import java.text.SimpleDateFormat
import java.util.*

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("已下载") },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    // 加载中
                    LoadingState()
                }
                downloads.isEmpty() -> {
                    // 空状态
                    EmptyState()
                }
                else -> {
                    // 下载列表
                    DownloadList(
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
 * 下载列表
 */
@Composable
private fun DownloadList(
    downloads: List<DownloadItem>,
    isFileExists: (String) -> Boolean,
    onDownloadClick: (DownloadItem) -> Unit,
    onDeleteClick: (DownloadItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = downloads,
            key = { it.id }
        ) { download ->
            DownloadItemCard(
                download = download,
                fileExists = isFileExists(download.localPath),
                onClick = { onDownloadClick(download) },
                onDelete = { onDeleteClick(download) }
            )
        }
    }
}

/**
 * 下载项卡片
 */
@Composable
private fun DownloadItemCard(
    download: DownloadItem,
    fileExists: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        onClick = onClick,
        enabled = fileExists
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Icon(
                imageVector = if (fileExists) Icons.Default.PlayArrow else Icons.Default.VideoFile,
                contentDescription = if (fileExists) "视频" else "文件已丢失",
                tint = if (fileExists) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 视频信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.videoTitle.ifEmpty { "未命名视频" },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (fileExists) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 文件大小和下载时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatFileSize(download.fileSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "下载于 ${formatDate(download.downloadedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // 文件丢失提示
                if (!fileExists) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "文件已丢失，请重新下载",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // 删除按钮
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除下载",
                    tint = MaterialTheme.colorScheme.error
                )
            }
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
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无下载",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "在文件浏览器中点击下载按钮保存视频",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
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

/**
 * 格式化日期
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
