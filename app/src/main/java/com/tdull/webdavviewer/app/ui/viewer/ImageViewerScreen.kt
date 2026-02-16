package com.tdull.webdavviewer.app.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import java.net.URLDecoder

/**
 * 图片查看器界面
 * 支持缩放和平移手势
 */
@Composable
fun ImageViewerScreen(
    imageUrl: String,
    imageTitle: String = "",
    onBack: () -> Unit
) {
    // 解码URL（导航传递时编码了）
    val decodedUrl = remember(imageUrl) {
        try {
            URLDecoder.decode(imageUrl, "UTF-8")
        } catch (e: Exception) {
            imageUrl
        }
    }
    val decodedTitle = remember(imageTitle) {
        try {
            URLDecoder.decode(imageTitle, "UTF-8")
        } catch (e: Exception) {
            imageTitle
        }
    }

    // 缩放和平移状态
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // 控制栏显示状态
    var showControls by remember { mutableStateOf(true) }
    
    // 图片尺寸状态（用于计算缩放边界）
    var imageSize by remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 图片显示
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(decodedUrl)
                .crossfade(true)
                .memoryCacheKey(decodedUrl)
                .diskCacheKey(decodedUrl)
                .build(),
            contentDescription = decodedTitle,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size -> 
                    imageSize = size.toSize()
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // 限制缩放范围在0.5x到5x之间
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            // 缩放为1时重置偏移
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls },
                        onDoubleTap = {
                            // 双击重置缩放
                            if (scale != 1f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = 2f
                            }
                        }
                    )
                },
            onState = { state ->
                when (state) {
                    is AsyncImagePainter.State.Loading -> {
                        // 加载中
                    }
                    is AsyncImagePainter.State.Success -> {
                        // 加载成功
                    }
                    is AsyncImagePainter.State.Error -> {
                        // 加载失败
                    }
                    else -> {}
                }
            }
        )
        
        // 加载状态叠加层
        // 由于 AsyncImage 本身会处理加载状态，这里可以简化

        // 控制层
        if (showControls) {
            ImageViewerControls(
                title = decodedTitle,
                onBack = onBack,
                onReset = {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                },
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

/**
 * 图片查看器控制层
 */
@Composable
private fun ImageViewerControls(
    title: String,
    onBack: () -> Unit,
    onReset: () -> Unit,
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
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            // 重置按钮
            TextButton(onClick = onReset) {
                Text("重置", color = Color.White)
            }
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
