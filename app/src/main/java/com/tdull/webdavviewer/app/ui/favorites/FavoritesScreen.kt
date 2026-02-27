package com.tdull.webdavviewer.app.ui.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tdull.webdavviewer.app.data.model.FavoriteItem
import com.tdull.webdavviewer.app.ui.browser.FileItem
import com.tdull.webdavviewer.app.viewmodel.FavoritesViewModel
import com.tdull.webdavviewer.app.data.model.ResourceType
import com.tdull.webdavviewer.app.data.model.WebDAVResource
import java.text.SimpleDateFormat
import java.util.*

/**
 * 收藏列表页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = hiltViewModel(),
    onVideoClick: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val videoPreviews by viewModel.videoPreviews.collectAsStateWithLifecycle()

    // 全屏预览图状态
    var previewState by remember { mutableStateOf<PreviewState?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的收藏") },
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
                uiState.showEmptyState -> {
                    // 空状态
                    EmptyState()
                }
                else -> {
                    // 收藏列表
                    FavoriteList(
                        favorites = uiState.favorites,
                        videoPreviews = videoPreviews,
                        onFavoriteClick = { favorite ->
                            handleFavoriteClick(
                                favorite = favorite,
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
                        onDeleteFavorite = { id ->
                            viewModel.removeFavorite(id)
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
 * 收藏列表
 */
@Composable
private fun FavoriteList(
    favorites: List<FavoriteItem>,
    videoPreviews: Map<String, List<String>>,
    onFavoriteClick: (FavoriteItem) -> Unit,
    onPreviewClick: (List<String>, Int) -> Unit,
    onLoadPreviews: (String) -> Unit,
    onDeleteFavorite: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = favorites,
            key = { it.id }
        ) { favorite ->
            // 获取预览图
            val previews = videoPreviews[favorite.resourcePath] ?: emptyList()

            FavoriteItemCard(
                favorite = favorite,
                previewImages = previews,
                onClick = { onFavoriteClick(favorite) },
                onPreviewClick = { index -> onPreviewClick(previews, index) },
                onLoadPreviews = { onLoadPreviews(favorite.resourcePath) },
                onDelete = { onDeleteFavorite(favorite.id) }
            )
        }
    }
}

/**
 * 收藏项卡片
 */
@Composable
private fun FavoriteItemCard(
    favorite: FavoriteItem,
    previewImages: List<String>,
    onClick: () -> Unit,
    onPreviewClick: (Int) -> Unit,
    onLoadPreviews: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 自动触发预览图加载
    LaunchedEffect(favorite.id) {
        onLoadPreviews()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column {
            // 主内容区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "视频",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 视频信息
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = favorite.videoTitle.ifEmpty { "未命名视频" },
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 收藏时间
                    Text(
                        text = "收藏于 ${formatDate(favorite.addedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // 删除按钮
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除收藏",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // 预览图区域
            if (previewImages.isNotEmpty()) {
                PreviewImagesRow(
                    images = previewImages.take(6),
                    onImageClick = onPreviewClick
                )
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个收藏吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 预览图横向列表
 */
@Composable
private fun PreviewImagesRow(
    images: List<String>,
    onImageClick: (Int) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier
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
    onClick: () -> Unit
) {
    coil.compose.AsyncImage(
        model = imageUrl,
        contentDescription = "视频预览图",
        modifier = Modifier
            .size(width = 100.dp, height = 56.dp)
            .clickable(onClick = onClick),
        contentScale = androidx.compose.ui.layout.ContentScale.Crop
    )
}

/**
 * 处理收藏项点击
 */
private fun handleFavoriteClick(
    favorite: FavoriteItem,
    viewModel: FavoritesViewModel,
    onVideoClick: (String) -> Unit,
    onImageClick: (String) -> Unit
) {
    // 直接使用收藏项中保存的 videoUrl 播放
    onVideoClick(favorite.videoUrl)
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
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无收藏",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "在视频播放器中点击收藏按钮添加",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * 全屏预览图对话框
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ImagePreviewDialog(
    images: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    // 简化实现，显示单张图片预览
    var currentIndex by remember { mutableIntStateOf(initialIndex) }

    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            coil.compose.AsyncImage(
                model = images.getOrNull(currentIndex),
                contentDescription = "预览图",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )

            // 左右切换按钮
            if (images.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0
                    ) {
                        Text("上一张", color = androidx.compose.ui.graphics.Color.White)
                    }
                    Text(
                        text = "${currentIndex + 1} / ${images.size}",
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    TextButton(
                        onClick = { if (currentIndex < images.size - 1) currentIndex++ },
                        enabled = currentIndex < images.size - 1
                    ) {
                        Text("下一张", color = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        }
    }
}

/**
 * 格式化日期
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
