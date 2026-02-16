package com.tdull.webdavviewer.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.tdull.webdavviewer.app.ui.theme.PlayerTheme
import com.tdull.webdavviewer.app.viewmodel.VideoPlayerViewModel

/**
 * 视频播放器界面
 * 使用 ExoPlayer 播放网络视频流
 */
@Composable
fun VideoPlayerScreen(
    videoUrl: String,
    videoTitle: String = "",
    onBack: () -> Unit,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val player by viewModel.player.collectAsStateWithLifecycle()
    
    // 初始化播放器
    LaunchedEffect(videoUrl) {
        viewModel.initializePlayer(videoUrl)
    }
    
    // 控制栏显示状态
    var showControls by remember { mutableStateOf(true) }
    
    PlayerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 视频播放器视图
            player?.let { exoPlayer ->
                VideoPlayerView(
                    player = exoPlayer,
                    modifier = Modifier.fillMaxSize(),
                    onClick = { showControls = !showControls }
                )
            }
            
            // 控制层
            if (showControls) {
                VideoPlayerControls(
                    title = videoTitle,
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }
    }
}

/**
 * ExoPlayer 视图
 */
@Composable
private fun VideoPlayerView(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false // 使用自定义控制器
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() },
        update = { playerView ->
            playerView.player = player
        }
    )
}

/**
 * 视频播放器控制层
 */
@Composable
private fun VideoPlayerControls(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(
                color = Color.Black.copy(alpha = 0.5f)
            )
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
            
            // 视频标题
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

/**
 * 简化的播放器加载状态指示器
 */
@Composable
fun VideoPlayerLoadingIndicator(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

/**
 * 视频播放错误提示
 */
@Composable
fun VideoPlayerError(
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
                text = "播放出错",
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
