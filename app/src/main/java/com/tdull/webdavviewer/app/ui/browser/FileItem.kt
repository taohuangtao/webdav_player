package com.tdull.webdavviewer.app.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tdull.webdavviewer.app.data.model.DownloadState
import com.tdull.webdavviewer.app.data.model.ResourceType
import com.tdull.webdavviewer.app.data.model.WebDAVResource
import com.tdull.webdavviewer.app.ui.components.MenuPopupContainer
import java.text.SimpleDateFormat
import java.util.*

// ================= 文件浏览器设计稿配色（filebrowser_redesign.html） =================
private val TextPrimary = Color(0xFF111827)      // 主文字
private val TextSecondary = Color(0xFF6B7280)    // 次级文字
private val TextMuted = Color(0xFF9CA3AF)        // 弱化文字
private val IndigoPrimary = Color(0xFF4F46E5)    // indigo 主色
private val IndigoLight = Color(0xFFEEF2FF)      // indigo 浅底
private val RosePrimary = Color(0xFFF43F5E)      // 视频红
private val RoseLight = Color(0xFFFFF1F2)        // 视频红浅底
private val SkyPrimary = Color(0xFF0EA5E9)       // 图片蓝
private val SkyLight = Color(0xFFF0F9FF)         // 图片蓝浅底
private val AmberPrimary = Color(0xFFF59E0B)     // 文件橙
private val AmberLight = Color(0xFFFFFBEB)       // 文件橙浅底

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
    downloadState: DownloadState = DownloadState.NotDownloaded,
    onCancelDownload: () -> Unit = {},
    moreMenuContent: (@Composable (onDismiss: () -> Unit) -> Unit)? = null,
    fileMissing: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val icon = getResourceIcon(resource.resourceType)
    val iconColor = getResourceIconColor(resource.resourceType)
    val hasPreviews = resource.isVideo && previewImages.isNotEmpty()
    
    // 视频文件时，自动触发预览图加载
    LaunchedEffect(resource.isVideo, resource.path) {
        if (resource.isVideo && previewImages.isEmpty()) {
            onLoadPreviews()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标（彩色浅底方块，设计稿风格）
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(getResourceIconBg(resource.resourceType), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = getResourceTypeName(resource.resourceType),
                    tint = iconColor,
                    modifier = Modifier.size(17.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
                
                // 文件信息
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = resource.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(3.dp))
                    
                    // 文件大小和修改时间
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 文件大小
                        if (!resource.isDirectory) {
                            Text(
                                text = formatFileSize(resource.size),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        
                        // 修改时间
                        if (resource.lastModified > 0) {
                            Text(
                                text = formatDate(resource.lastModified),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // 文件丢失提示（下载页已下载项本地文件缺失时显示）
                    if (fileMissing) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "文件已丢失，请重新下载",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                // 目录：仅显示更多操作按钮（点击 item 本身即进入）
                if (resource.isDirectory) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 更多操作按钮（含下拉菜单，锚定到按钮处）
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "更多操作",
                                    tint = TextSecondary
                                )
                            }
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
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 下载中进度指示（仅下载中显示，点击取消）
                        if (resource.isVideo && downloadState is DownloadState.Downloading) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable(onClick = onCancelDownload)
                            ) {
                                CircularProgressIndicator(
                                    progress = { downloadState.progressPercent / 100f },
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.5.dp,
                                    color = IndigoPrimary
                                )
                                Text(
                                    text = "${downloadState.progressPercent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IndigoPrimary
                                )
                            }
                        }
                        // 更多操作按钮（含下拉菜单，锚定到按钮处）
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "更多操作",
                                    tint = TextSecondary
                                )
                            }
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
        ResourceType.DIRECTORY -> Icons.Filled.Folder
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
        ResourceType.DIRECTORY -> IndigoPrimary
        ResourceType.VIDEO -> RosePrimary
        ResourceType.IMAGE -> SkyPrimary
        ResourceType.AUDIO -> IndigoPrimary
        ResourceType.OTHER -> AmberPrimary
    }
}

/**
 * 根据资源类型获取图标浅底色（设计稿彩色浅底方块）
 */
private fun getResourceIconBg(type: ResourceType): Color {
    return when (type) {
        ResourceType.DIRECTORY -> IndigoLight
        ResourceType.VIDEO -> RoseLight
        ResourceType.IMAGE -> SkyLight
        ResourceType.AUDIO -> IndigoLight
        ResourceType.OTHER -> AmberLight
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
