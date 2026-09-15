package com.tdull.webdavviewer.app.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import java.net.URLDecoder
import android.util.Log

private enum class ImageLoadSource {
    MEDIUM_THUMBNAIL,
    ORIGINAL
}

data class ImageViewerItem(
    val url: String,
    val mediumThumbnailUrl: String = "",
    val title: String = ""
)

private data class ImagePageUiState(
    val thumbnailLoaded: Boolean = false,
    val originalLoaded: Boolean = false,
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val currentSource: ImageLoadSource = ImageLoadSource.ORIGINAL
)

/**
 * 图片查看器界面
 * 支持缩放和平移手势
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageViewerScreen(
    imageUrl: String,
    mediumThumbnailUrl: String = "",
    imageTitle: String = "",
    items: List<ImageViewerItem> = emptyList(),
    initialIndex: Int = 0,
    onBack: () -> Unit
) {
    // 解码URL（导航传递时编码了）
    val decodedUrl = remember(imageUrl) { decodeNavigationValue(imageUrl) }
    val decodedMediumThumbnailUrl = remember(mediumThumbnailUrl) {
        decodeNavigationValue(mediumThumbnailUrl)
    }
    val decodedTitle = remember(imageTitle) { decodeNavigationValue(imageTitle) }
    val viewerItems = remember(items, decodedUrl, decodedMediumThumbnailUrl, decodedTitle) {
        items.ifEmpty {
            listOf(
                ImageViewerItem(
                    url = decodedUrl,
                    mediumThumbnailUrl = decodedMediumThumbnailUrl,
                    title = decodedTitle
                )
            )
        }
    }
    val initialPage = remember(viewerItems, initialIndex) {
        initialIndex.coerceIn(0, viewerItems.lastIndex)
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { viewerItems.size }
    )
    val pageStates = remember(viewerItems) {
        mutableStateMapOf<Int, ImagePageUiState>()
    }
    val originalRequestVersions = remember(viewerItems) {
        mutableStateMapOf<Int, Int>()
    }

    // 缩放和平移状态
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    fun resetTransform() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    // 控制栏显示状态
    var showControls by remember { mutableStateOf(true) }
    val currentPage = pagerState.currentPage.coerceIn(0, viewerItems.lastIndex)
    val currentItem = viewerItems[currentPage]
    val currentPageState = pageStates[currentPage] ?: initialImagePageUiState(currentItem)

    LaunchedEffect(viewerItems, initialPage) {
        pagerState.scrollToPage(initialPage)
    }

    LaunchedEffect(pagerState.currentPage) {
        resetTransform()
    }

    // 隐藏状态栏和导航栏，实现全屏沉浸式体验
    val context = LocalContext.current
    val view = LocalView.current
    val window = (context as? android.app.Activity)?.window

    DisposableEffect(Unit) {
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, view)
            // 隐藏状态栏和导航栏
            controller.hide(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
            )
            // 设置系统栏行为：滑动时暂时显示，然后自动隐藏
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, view).run {
                    show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                    isAppearanceLightStatusBars = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = scale <= 1f,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = viewerItems[page]
            val isCurrentPage = page == currentPage

            ImageViewerPage(
                item = item,
                scale = if (isCurrentPage) scale else 1f,
                offsetX = if (isCurrentPage) offsetX else 0f,
                offsetY = if (isCurrentPage) offsetY else 0f,
                originalRequestVersion = originalRequestVersions[page] ?: 0,
                onTransform = { pan, zoom ->
                    if (isCurrentPage) {
                        val nextScale = (scale * zoom).coerceIn(0.5f, 5f)
                        scale = nextScale
                        if (nextScale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                },
                onTap = { showControls = !showControls },
                onDoubleTap = {
                    if (isCurrentPage) {
                        if (scale != 1f) {
                            resetTransform()
                        } else {
                            scale = 2f
                        }
                    }
                },
                onResetTransform = { if (isCurrentPage) resetTransform() },
                onStateChange = { state ->
                    pageStates[page] = state
                }
            )
        }

        // 控制层
        if (showControls) {
            ImageViewerControls(
                title = currentItem.title,
                pageIndicator = if (viewerItems.size > 1) {
                    "${currentPage + 1} / ${viewerItems.size}"
                } else {
                    null
                },
                onBack = onBack,
                onReset = {
                    resetTransform()
                },
                showOriginalButton = currentPageState.thumbnailLoaded,
                isOriginalLoading = currentPageState.currentSource == ImageLoadSource.ORIGINAL &&
                    currentPageState.isLoading,
                isOriginalLoaded = currentPageState.originalLoaded,
                onLoadOriginal = {
                    originalRequestVersions[currentPage] =
                        (originalRequestVersions[currentPage] ?: 0) + 1
                    resetTransform()
                },
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

private fun initialImagePageUiState(item: ImageViewerItem): ImagePageUiState {
    return ImagePageUiState(
        currentSource = if (item.mediumThumbnailUrl.isNotBlank()) {
            ImageLoadSource.MEDIUM_THUMBNAIL
        } else {
            ImageLoadSource.ORIGINAL
        }
    )
}

@Composable
private fun ImageViewerPage(
    item: ImageViewerItem,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    originalRequestVersion: Int,
    onTransform: (pan: Offset, zoom: Float) -> Unit,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onResetTransform: () -> Unit,
    onStateChange: (ImagePageUiState) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSource by remember(item) {
        mutableStateOf(
            if (item.mediumThumbnailUrl.isNotBlank()) {
                ImageLoadSource.MEDIUM_THUMBNAIL
            } else {
                ImageLoadSource.ORIGINAL
            }
        )
    }
    var thumbnailLoaded by remember(item) { mutableStateOf(false) }
    var originalLoaded by remember(item) { mutableStateOf(false) }
    var isLoading by remember(item) { mutableStateOf(true) }
    var loadError by remember(item) { mutableStateOf<String?>(null) }
    var retryKey by remember(item) { mutableIntStateOf(0) }
    val currentImageUrl = if (currentSource == ImageLoadSource.MEDIUM_THUMBNAIL) {
        item.mediumThumbnailUrl
    } else {
        item.url
    }

    LaunchedEffect(originalRequestVersion) {
        if (originalRequestVersion > 0 && currentSource != ImageLoadSource.ORIGINAL) {
            currentSource = ImageLoadSource.ORIGINAL
            originalLoaded = false
            loadError = null
            isLoading = true
            onResetTransform()
        }
    }

    LaunchedEffect(thumbnailLoaded, originalLoaded, isLoading, loadError, currentSource) {
        onStateChange(
            ImagePageUiState(
                thumbnailLoaded = thumbnailLoaded,
                originalLoaded = originalLoaded,
                isLoading = isLoading,
                loadError = loadError,
                currentSource = currentSource
            )
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(currentImageUrl)
                .crossfade(true)
                .memoryCacheKey("$currentImageUrl#$retryKey")
                .diskCacheKey(currentImageUrl)
                .build(),
            contentDescription = item.title.ifBlank { "图片" },
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .pointerInput(scale) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val pressedCount = event.changes.count { it.pressed }
                            val shouldHandleTransform = scale > 1f || pressedCount > 1

                            if (shouldHandleTransform) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                if (zoom != 1f || pan != Offset.Zero) {
                                    onTransform(pan, zoom)
                                    event.changes.forEach { change ->
                                        if (change.positionChanged()) {
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = { onDoubleTap() }
                    )
                },
            onState = { state ->
                Log.d("ImageViewerScreen", "onState: $state")
                when (state) {
                    is AsyncImagePainter.State.Loading -> {
                        isLoading = true
                        loadError = null
                    }
                    is AsyncImagePainter.State.Success -> {
                        isLoading = false
                        loadError = null
                        if (currentSource == ImageLoadSource.MEDIUM_THUMBNAIL) {
                            thumbnailLoaded = true
                        } else {
                            originalLoaded = true
                        }
                    }
                    is AsyncImagePainter.State.Error -> {
                        isLoading = false
                        if (currentSource == ImageLoadSource.MEDIUM_THUMBNAIL) {
                            currentSource = ImageLoadSource.ORIGINAL
                            onResetTransform()
                            loadError = null
                            isLoading = true
                        } else {
                            loadError = "图片加载失败"
                        }
                    }
                    else -> {}
                }
            }
        )

        if (isLoading) {
            ImageLoadingOverlay(
                text = if (currentSource == ImageLoadSource.ORIGINAL && thumbnailLoaded) {
                    "正在加载原图..."
                } else {
                    "图片加载中..."
                }
            )
        }

        loadError?.let { error ->
            ImageLoadError(
                errorMessage = error,
                onRetry = {
                    retryKey++
                    isLoading = true
                    loadError = null
                }
            )
        }
    }
}

private fun decodeNavigationValue(value: String): String {
    return try {
        URLDecoder.decode(value, "UTF-8")
    } catch (e: Exception) {
        value
    }
}

/**
 * 图片查看器控制层
 */
@Composable
private fun ImageViewerControls(
    title: String,
    pageIndicator: String?,
    onBack: () -> Unit,
    onReset: () -> Unit,
    showOriginalButton: Boolean,
    isOriginalLoading: Boolean,
    isOriginalLoaded: Boolean,
    onLoadOriginal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        // 顶部控制栏
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 返回按钮
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }

            // 图片标题
            Text(
                text = if (pageIndicator != null) {
                    "$title  $pageIndicator"
                } else {
                    title
                },
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            if (showOriginalButton) {
                TextButton(
                    onClick = onLoadOriginal,
                    enabled = !isOriginalLoading && !isOriginalLoaded
                ) {
                    if (isOriginalLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isOriginalLoaded) "已加载原图" else "原图",
                        color = if (isOriginalLoaded) {
                            Color.White.copy(alpha = 0.6f)
                        } else {
                            Color.White
                        }
                    )
                }
            }

            // 重置按钮
            TextButton(onClick = onReset) {
                Text("重置", color = Color.White)
            }
        }
    }
}

/**
 * 图片加载动画
 */
@Composable
private fun ImageLoadingOverlay(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * 图片加载错误提示
 */
@Composable
fun ImageLoadError(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "加载失败",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = errorMessage,
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onRetry) {
                Text(text = "重试")
            }
        }
    }
}
