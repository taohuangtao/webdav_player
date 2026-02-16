package com.tdull.webdavviewer.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tdull.webdavviewer.app.util.ErrorInfo
import com.tdull.webdavviewer.app.util.ErrorType

/**
 * 通用错误显示组件
 * 根据错误类型显示不同的图标和提示信息
 */
@Composable
fun ErrorView(
    errorInfo: ErrorInfo,
    onRetry: () -> Unit,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 错误图标
            Icon(
                imageVector = getErrorIcon(errorInfo.type),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = when (errorInfo.type) {
                    ErrorType.AUTHENTICATION_FAILED -> MaterialTheme.colorScheme.error
                    ErrorType.NETWORK_UNAVAILABLE -> MaterialTheme.colorScheme.outline
                    else -> MaterialTheme.colorScheme.error
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 错误标题
            Text(
                text = errorInfo.title,
                style = MaterialTheme.typography.titleLarge,
                color = when (errorInfo.type) {
                    ErrorType.NETWORK_UNAVAILABLE -> MaterialTheme.colorScheme.outline
                    else -> MaterialTheme.colorScheme.error
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 错误消息
            Text(
                text = errorInfo.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 重试按钮
                if (errorInfo.canRetry) {
                    Button(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("重试")
                    }
                }
                
                // 附加操作按钮
                if (errorInfo.actionText != null && onAction != null) {
                    OutlinedButton(onClick = onAction) {
                        Text(errorInfo.actionText)
                    }
                }
            }
        }
    }
}

/**
 * 简洁的错误提示组件（用于卡片或列表项）
 */
@Composable
fun CompactErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

/**
 * 加载状态组件
 */
@Composable
fun LoadingView(
    message: String = "加载中…",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * 空状态组件
 */
@Composable
fun EmptyStateView(
    icon: ImageVector = Icons.Default.Folder,
    title: String,
    message: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline
            )
            if (message != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onAction) {
                    Text(actionText)
                }
            }
        }
    }
}

/**
 * 根据错误类型获取对应图标
 */
private fun getErrorIcon(errorType: ErrorType): ImageVector {
    return when (errorType) {
        ErrorType.NETWORK_UNAVAILABLE -> Icons.Default.WifiOff
        ErrorType.CONNECTION_TIMEOUT -> Icons.Default.HourglassEmpty
        ErrorType.SERVER_UNREACHABLE -> Icons.Default.CloudOff
        ErrorType.AUTHENTICATION_FAILED -> Icons.Default.Lock
        ErrorType.RESOURCE_NOT_FOUND -> Icons.Default.SearchOff
        ErrorType.UNSUPPORTED_FORMAT -> Icons.Default.BrokenImage
        ErrorType.SERVER_ERROR -> Icons.Default.Error
        ErrorType.INVALID_RESPONSE -> Icons.Default.DataObject
        ErrorType.UNKNOWN -> Icons.Default.HelpOutline
    }
}
