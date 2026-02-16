package com.tdull.webdavviewer.app.ui.browser

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 面包屑导航组件
 * 将路径拆分为层级，每个层级可点击跳转
 */
@Composable
fun Breadcrumb(
    path: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pathSegments = remember(path) { parsePathSegments(path) }
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 根目录
        BreadcrumbItem(
            text = "/",
            isLast = pathSegments.isEmpty(),
            onClick = { onNavigate("/") }
        )

        // 路径层级
        pathSegments.forEachIndexed { index, segment ->
            // 分隔符
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.outline
            )

            // 计算当前层级的完整路径
            val segmentPath = buildSegmentPath(pathSegments, index)
            val isLast = index == pathSegments.size - 1

            BreadcrumbItem(
                text = segment,
                isLast = isLast,
                onClick = { onNavigate(segmentPath) }
            )
        }
    }
}

/**
 * 面包屑单项
 */
@Composable
private fun BreadcrumbItem(
    text: String,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        enabled = !isLast,
        modifier = modifier.padding(horizontal = 2.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = if (isLast) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = if (isLast) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            }
        )
    }
}

/**
 * 解析路径段
 * 例如: "/Videos/Movies/" -> ["Videos", "Movies"]
 */
private fun parsePathSegments(path: String): List<String> {
    if (path.isEmpty() || path == "/") return emptyList()
    
    return path
        .trim('/')
        .split("/")
        .filter { it.isNotEmpty() }
}

/**
 * 构建到指定索引的路径
 * 例如: pathSegments = ["Videos", "Movies"], index = 0 -> "/Videos/"
 */
private fun buildSegmentPath(pathSegments: List<String>, index: Int): String {
    val builder = StringBuilder("/")
    
    for (i in 0..index) {
        builder.append(pathSegments[i])
        if (i < index) {
            builder.append("/")
        }
    }
    
    // 确保以 / 结尾
    val result = builder.toString()
    return if (result.endsWith("/")) result else "$result/"
}
