package com.tdull.webdavviewer.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
import com.tdull.webdavviewer.app.viewmodel.VideoPlayerUiState
import kotlinx.coroutines.delay

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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 初始化播放器
    LaunchedEffect(videoUrl) {
        viewModel.initializePlayer(videoUrl)
    }

    // 控制栏显示状态
    var showControls by remember { mutableStateOf(true) }

    // 自动隐藏控制栏
    LaunchedEffect(showControls, uiState.isPlaying) {
        if (showControls && uiState.isPlaying) {
            delay(3000)
            showControls = false
        }
    }

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
                // 顶部控制栏
                VideoPlayerTopControls(
                    title = videoTitle,
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.TopStart)
                )

                // 底部控制栏
                VideoPlayerBottomControls(
                    uiState = uiState,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onReplayClick = { viewModel.replay() },
                    onSeek = { position -> viewModel.seekTo(position) },
                    onVolumeChange = { volume -> viewModel.setVolume(volume) },
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }

            // 加载指示器
            VideoPlayerLoadingIndicator(
                isLoading = uiState.isLoading,
                modifier = Modifier.align(Alignment.Center)
            )

            // 错误提示
            uiState.error?.let { error ->
                VideoPlayerError(
                    errorMessage = error,
                    onRetry = { viewModel.retry() },
                    modifier = Modifier.align(Alignment.Center)
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
 * 视频播放器顶部控制栏
 */
@Composable
private fun VideoPlayerTopControls(
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
 * 视频播放器底部控制栏
 */
@Composable
private fun VideoPlayerBottomControls(
    uiState: VideoPlayerUiState,
    onPlayPauseClick: () -> Unit,
    onReplayClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableLongStateOf(0L) }
    var showVolumeSlider by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(
                color = Color.Black.copy(alpha = 0.5f)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        // 进度条
        VideoProgressSlider(
            position = if (isSeeking) seekPosition else uiState.currentPosition,
            duration = uiState.duration,
            onValueChange = { position ->
                isSeeking = true
                seekPosition = position
            },
            onValueChangeFinished = {
                onSeek(seekPosition)
                isSeeking = false
            }
        )

        // 控制按钮和时间显示
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 播放/暂停按钮
            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // 重新播放按钮（播放结束时显示）
            if (uiState.isPlaybackEnded) {
                IconButton(onClick = onReplayClick) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "重新播放",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 当前时间
            Text(
                text = formatTime(uiState.currentPosition),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = " / ",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )

            // 总时长
            Text(
                text = formatTime(uiState.duration),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.weight(1f))

            // 音量控制
            Box {
                // 音量按钮
                IconButton(onClick = { showVolumeSlider = !showVolumeSlider }) {
                    Icon(
                        imageVector = when {
                            uiState.volume <= 0f -> Icons.Default.VolumeOff
                            uiState.volume < 0.5f -> Icons.Default.VolumeDown
                            else -> Icons.Default.VolumeUp
                        },
                        contentDescription = "音量",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 音量滑块弹出层
                if (showVolumeSlider) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { showVolumeSlider = false },
                        modifier = Modifier
                            .width(200.dp)
                            .background(Color.Black.copy(alpha = 0.9f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "音量",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = uiState.volume,
                                onValueChange = { volume ->
                                    onVolumeChange(volume)
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                            Text(
                                text = "${(uiState.volume * 100).toInt()}%",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 视频进度条
 */
@Composable
private fun VideoProgressSlider(
    position: Long,
    duration: Long,
    onValueChange: (Long) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val progress = if (duration > 0) (position.toFloat() / duration.toFloat()) else 0f

    Slider(
        value = progress,
        onValueChange = { value ->
            onValueChange((value * duration).toLong())
        },
        onValueChangeFinished = onValueChangeFinished,
        valueRange = 0f..1f,
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
        )
    )
}

/**
 * 格式化时间为 mm:ss 或 HH:mm:ss
 */
private fun formatTime(milliseconds: Long): String {
    if (milliseconds < 0) return "00:00"

    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
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
