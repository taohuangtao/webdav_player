package com.tdull.webdavviewer.app.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tdull.webdavviewer.app.data.model.ResourceType
import com.tdull.webdavviewer.app.data.model.WebDAVResource
import java.text.SimpleDateFormat
import java.util.*

/**
 * 文件列表项组件
 */
@Composable
fun FileItem(
    resource: WebDAVResource,
    onClick: () -> Unit,
    previewImages: List<String> = emptyList(),
    onPreviewClick: (List<String>, Int) -> Unit = { _, _ -> },
    onLoadPreviews: () -> Unit = {},
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val icon = getResourceIcon(resource.resourceType)
    val iconColor = getResourceIconColor(resource.resourceType)
    val hasPreviews = resource.isVideo && previewImages.isNotEmpty()
    
    // 视频文件时，自动触发预览图加载
    LaunchedEffect(resource.isVideo, resource.path) {
        if (resource.isVideo && previewImages.isEmpty()) {
            onLoadPreviews()
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标
                Icon(
                    imageVector = icon,
                    contentDescription = getResourceTypeName(resource.resourceType),
                    tint = iconColor,
                    modifier = Modifier.size(40.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 文件信息
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = resource.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 文件大小和修改时间
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 文件大小
                        if (!resource.isDirectory) {
                            Text(
                                text = formatFileSize(resource.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        
                        // 修改时间
                        if (resource.lastModified > 0) {
                            Text(
                                text = formatDate(resource.lastModified),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                
                // 目录箭头
                if (resource.isDirectory) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "进入目录",
                        tint = MaterialTheme.colorScheme.outline
                    )
                } else {
                    // 收藏按钮
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.Star,
                            contentDescription = if (isFavorite) "取消收藏" else "收藏",
                            tint = if (isFavorite) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            
            // 视频预览图区域
            if (hasPreviews) {
                PreviewImagesRow(
                    images = previewImages.take(6),
                    onImageClick = { index ->
                        onPreviewClick(previewImages, index)
                    }
                )
            }
        }
    }
}

/**
 * 预览图横向列表
 */
@Composable
private fun PreviewImagesRow(
    images: List<String>,
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = images, key = { it }) { imageUrl ->
            PreviewImageItem(
                imageUrl = imageUrl,
                onClick = { onImageClick(images.indexOf(imageUrl)) }
            )
        }
    }
}

/**
 * 单个预览图项
 */
@Composable
private fun PreviewImageItem(
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = imageUrl,
        contentDescription = "视频预览图",
        modifier = modifier
            .size(width = 100.dp, height = 56.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop
    )
}

/**
 * 根据资源类型获取图标
 */
@Composable
private fun getResourceIcon(type: ResourceType): ImageVector {
    return when (type) {
        ResourceType.DIRECTORY -> Icons.AutoMirrored.Filled.List
        ResourceType.VIDEO -> Icons.Default.PlayArrow
        ResourceType.IMAGE -> Icons.Default.Person
        ResourceType.AUDIO -> Icons.Default.Phone
        ResourceType.OTHER -> Icons.Default.Info
    }
}

/**
 * 根据资源类型获取图标颜色
 */
@Composable
private fun getResourceIconColor(type: ResourceType): androidx.compose.ui.graphics.Color {
    return when (type) {
        ResourceType.DIRECTORY -> MaterialTheme.colorScheme.primary
        ResourceType.VIDEO -> MaterialTheme.colorScheme.tertiary
        ResourceType.IMAGE -> MaterialTheme.colorScheme.secondary
        ResourceType.AUDIO -> MaterialTheme.colorScheme.primary
        ResourceType.OTHER -> MaterialTheme.colorScheme.outline
    }
}

/**
 * 获取资源类型名称
 */
private fun getResourceTypeName(type: ResourceType): String {
    return when (type) {
        ResourceType.DIRECTORY -> "文件夹"
        ResourceType.VIDEO -> "视频"
        ResourceType.IMAGE -> "图片"
        ResourceType.AUDIO -> "音频"
        ResourceType.OTHER -> "文件"
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
