package com.tdull.webdavviewer.app.ui.uploads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.tdull.webdavviewer.app.data.model.UploadStatus
import com.tdull.webdavviewer.app.data.model.UploadTask
import com.tdull.webdavviewer.app.viewmodel.UploadsViewModel

private val SettingsBg = Color(0xFFF4F6FB)
private val CardWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF6B7280)
private val TextMuted = Color(0xFF9CA3AF)
private val IndigoPrimary = Color(0xFF4F46E5)
private val IndigoLight = Color(0xFFEEF2FF)
private val ErrorPrimary = Color(0xFFF43F5E)
private val ErrorLight = Color(0xFFFFF1F2)
private val SuccessPrimary = Color(0xFF059669)
private val SuccessLight = Color(0xFFECFDF5)
private val WarningPrimary = Color(0xFFF59E0B)
private val WarningLight = Color(0xFFFFFBEB)
private val DividerColor = Color(0xFFF3F4F6)

@Composable
fun UploadsScreen(
    viewModel: UploadsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uploads by viewModel.uploads.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = SettingsBg,
        topBar = {
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
                        text = "上传任务",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (uploads.any { it.isFinished }) {
                        IconButton(onClick = { viewModel.clearFinishedUploads() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "清除已结束任务",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uploads.isEmpty()) {
                EmptyState()
            } else {
                UploadList(
                    uploads = uploads,
                    onCancel = { task -> viewModel.cancelUpload(task.id) },
                    onPause = { task -> viewModel.pauseUpload(task.id) },
                    onContinue = { task -> viewModel.continueUpload(task.id) },
                    onRetry = { task -> viewModel.retryUpload(task.id) }
                )
            }
        }
    }

    uiState.error?.let { error ->
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

@Composable
private fun UploadList(
    uploads: List<UploadTask>,
    onCancel: (UploadTask) -> Unit,
    onPause: (UploadTask) -> Unit,
    onContinue: (UploadTask) -> Unit,
    onRetry: (UploadTask) -> Unit
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
        itemsIndexed(
            items = uploads,
            key = { _, item -> item.id }
        ) { index, task ->
            UploadTaskRow(
                task = task,
                onCancel = { onCancel(task) },
                onPause = { onPause(task) },
                onContinue = { onContinue(task) },
                onRetry = { onRetry(task) }
            )
            if (index < uploads.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 62.dp),
                    color = DividerColor
                )
            }
        }
    }
}

@Composable
private fun UploadTaskRow(
    task: UploadTask,
    onCancel: () -> Unit,
    onPause: () -> Unit,
    onContinue: () -> Unit,
    onRetry: () -> Unit
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
            StatusIcon(status = task.status)
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${task.serverName}${task.targetDirectory}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = statusText(task),
                    fontSize = 12.sp,
                    color = statusTextColor(task.status),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            TaskActions(
                status = task.status,
                onCancel = onCancel,
                onPause = onPause,
                onContinue = onContinue,
                onRetry = onRetry
            )
        }

        if (task.status == UploadStatus.RUNNING || task.status == UploadStatus.QUEUED) {
            Spacer(modifier = Modifier.height(8.dp))
            if (task.totalBytes > 0L) {
                LinearProgressIndicator(
                    progress = { task.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = IndigoPrimary,
                    trackColor = IndigoLight
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = IndigoPrimary,
                    trackColor = IndigoLight
                )
            }
        }
    }
}

@Composable
private fun StatusIcon(status: UploadStatus) {
    val (bg, tint, icon) = when (status) {
        UploadStatus.COMPLETED -> Triple(SuccessLight, SuccessPrimary, Icons.Default.Check)
        UploadStatus.FAILED -> Triple(ErrorLight, ErrorPrimary, Icons.Default.Warning)
        UploadStatus.CANCELED -> Triple(ErrorLight, ErrorPrimary, Icons.Default.Close)
        UploadStatus.PAUSED -> Triple(WarningLight, WarningPrimary, Icons.Default.Pause)
        UploadStatus.WAITING_CONFLICT -> Triple(WarningLight, WarningPrimary, Icons.Default.Warning)
        else -> Triple(IndigoLight, IndigoPrimary, Icons.Default.UploadFile)
    }

    Box(
        modifier = Modifier
            .size(34.dp)
            .background(bg, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (status == UploadStatus.RUNNING) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = tint
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun TaskActions(
    status: UploadStatus,
    onCancel: () -> Unit,
    onPause: () -> Unit,
    onContinue: () -> Unit,
    onRetry: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (status == UploadStatus.QUEUED || status == UploadStatus.RUNNING) {
            IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "暂停",
                    tint = IndigoPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (status == UploadStatus.PAUSED) {
            IconButton(onClick = onContinue, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "继续上传",
                    tint = IndigoPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (status == UploadStatus.FAILED || status == UploadStatus.WAITING_CONFLICT) {
            IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = if (status == UploadStatus.WAITING_CONFLICT) "覆盖重试" else "重试",
                    tint = IndigoPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (status == UploadStatus.QUEUED ||
            status == UploadStatus.RUNNING ||
            status == UploadStatus.PAUSED ||
            status == UploadStatus.FAILED ||
            status == UploadStatus.WAITING_CONFLICT
        ) {
            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "取消",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun statusText(task: UploadTask): String {
    return when (task.status) {
        UploadStatus.QUEUED -> task.errorMessage ?: "等待上传"
        UploadStatus.RUNNING -> if (task.totalBytes > 0L) {
            "${task.progressPercent}% · ${formatFileSize(task.uploadedBytes)} / ${formatFileSize(task.totalBytes)}"
        } else {
            "正在上传"
        }
        UploadStatus.PAUSED -> "已暂停"
        UploadStatus.WAITING_CONFLICT -> task.errorMessage ?: "目标文件已存在，可覆盖重试"
        UploadStatus.COMPLETED -> "上传完成"
        UploadStatus.FAILED -> task.errorMessage ?: "上传失败"
        UploadStatus.CANCELED -> "已取消"
    }
}

private fun statusTextColor(status: UploadStatus): Color {
    return when (status) {
        UploadStatus.COMPLETED -> SuccessPrimary
        UploadStatus.FAILED, UploadStatus.CANCELED -> ErrorPrimary
        UploadStatus.PAUSED, UploadStatus.WAITING_CONFLICT -> WarningPrimary
        else -> TextSecondary
    }
}

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
                imageVector = Icons.Default.UploadFile,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TextMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无上传任务",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "在文件浏览器顶部加号中选择上传文件",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatFileSize(size: Long): String {
    if (size < 0L) return "未知"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = size.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return String.format("%.1f %s", value, units[unitIndex])
}
