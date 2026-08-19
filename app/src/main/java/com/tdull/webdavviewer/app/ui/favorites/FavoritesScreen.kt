package com.tdull.webdavviewer.app.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tdull.webdavviewer.app.data.model.FavoriteItem
import com.tdull.webdavviewer.app.ui.browser.FileItem
import com.tdull.webdavviewer.app.ui.components.MenuItemRow
import com.tdull.webdavviewer.app.viewmodel.FavoritesViewModel

// ================= 收藏页设计稿配色（与 filebrowser_redesign.html 统一） =================
private val SettingsBg = Color(0xFFF4F6FB)      // 页面背景
private val CardWhite = Color(0xFFFFFFFF)        // 卡片底色
private val TextPrimary = Color(0xFF111827)      // 主文字
private val TextSecondary = Color(0xFF6B7280)    // 次级文字
private val TextMuted = Color(0xFF9CA3AF)        // 弱化文字
private val DeleteRed = Color(0xFFF43F5E)        // 删除红
private val DividerColor = Color(0xFFF3F4F6)     // 分割线

/**
 * 收藏列表页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = hiltViewModel(),
    onVideoClick: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val videoPreviews by viewModel.videoPreviews.collectAsStateWithLifecycle()

    // 全屏预览图状态
    var previewState by remember { mutableStateOf<PreviewState?>(null) }

    // 待删除的收藏项（由更多菜单触发删除确认）
    var deleteTarget by remember { mutableStateOf<FavoriteItem?>(null) }

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
                        text = "我的收藏",
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
                                onVideoClick = onVideoClick
                            )
                        },
                        onPreviewClick = { images, index ->
                            previewState = PreviewState(images, index)
                        },
                        onLoadPreviews = { path ->
                            viewModel.loadVideoPreviews(path)
                        },
                        onDeleteRequest = { favorite ->
                            deleteTarget = favorite
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

    // 删除确认对话框
    deleteTarget?.let { favorite ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个收藏吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeFavorite(favorite.id)
                        deleteTarget = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("取消")
                }
            }
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
 * 收藏列表（白卡片包裹，与文件浏览器一致）
 */
@Composable
private fun FavoriteList(
    favorites: List<FavoriteItem>,
    videoPreviews: Map<String, List<String>>,
    onFavoriteClick: (FavoriteItem) -> Unit,
    onPreviewClick: (List<String>, Int) -> Unit,
    onLoadPreviews: (String) -> Unit,
    onDeleteRequest: (FavoriteItem) -> Unit
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
            items = favorites,
            key = { _, item -> item.id }
        ) { index, favorite ->
            // 获取预览图
            val previews = videoPreviews[favorite.resourcePath] ?: emptyList()

            FileItem(
                resource = favorite.toWebDAVResource(),
                onClick = { onFavoriteClick(favorite) },
                previewImages = previews,
                onPreviewClick = { images, previewIndex -> onPreviewClick(images, previewIndex) },
                onLoadPreviews = { onLoadPreviews(favorite.resourcePath) },
                moreMenuContent = { onDismiss ->
                    MenuItemRow(
                        icon = Icons.Default.PlayArrow,
                        iconTint = TextPrimary,
                        text = "打开",
                        textColor = TextPrimary,
                        onClick = {
                            onDismiss()
                            onFavoriteClick(favorite)
                        }
                    )
                    MenuItemRow(
                        icon = Icons.Default.Delete,
                        iconTint = DeleteRed,
                        text = "删除收藏",
                        textColor = DeleteRed,
                        onClick = {
                            onDismiss()
                            onDeleteRequest(favorite)
                        }
                    )
                }
            )

            // 行间分割线（最后一行不加）
            if (index < favorites.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 62.dp),
                    color = DividerColor
                )
            }
        }
    }
}

/**
 * 处理收藏项点击
 */
private fun handleFavoriteClick(
    favorite: FavoriteItem,
    onVideoClick: (String) -> Unit
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
                tint = TextMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无收藏",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "在视频播放器中点击收藏按钮添加",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
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

    BasicAlertDialog(
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
