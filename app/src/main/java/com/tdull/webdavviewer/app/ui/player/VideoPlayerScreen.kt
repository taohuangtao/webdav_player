package com.tdull.webdavviewer.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.tdull.webdavviewer.app.viewmodel.VideoInfo
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
                    onSeekForward = { viewModel.seekForward() },
                    onSeekBackward = { viewModel.seekBackward() },
                    onSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
                    onShowVideoInfo = { viewModel.toggleVideoInfoDialog(true) },
                    onShowSettings = { viewModel.toggleSettingsDialog(true) },
                    onToggleFavorite = { viewModel.toggleFavorite(videoUrl, videoTitle) },
                    modifier = Modifier.align(Alignment.BottomStart)
                )

                // 视频信息弹窗
                if (uiState.showVideoInfoDialog) {
                    VideoInfoDialog(
                        videoInfo = uiState.videoInfo,
                        onDismiss = { viewModel.toggleVideoInfoDialog(false) }
                    )
                }

                // 设置弹窗
                if (uiState.showSettingsDialog) {
                    PlayerSettingsDialog(
                        seekSeconds = uiState.seekSeconds,
                        onSeekSecondsChange = { seconds -> viewModel.setSeekSeconds(seconds) },
                        onDismiss = { viewModel.toggleSettingsDialog(false) }
                    )
                }
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
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onShowVideoInfo: () -> Unit,
    onShowSettings: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableLongStateOf(0L) }
    var showVolumeSlider by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val speedOptions = listOf(0.5f, 0.7f, 1f, 1.5f, 2f, 3f, 4f)

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
            // 快退按钮
            IconButton(onClick = onSeekBackward) {
                Icon(
                    imageVector = Icons.Default.FastRewind,
                    contentDescription = "快退${uiState.seekSeconds}秒",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // 播放/暂停按钮
            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // 快进按钮
            IconButton(onClick = onSeekForward) {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = "快进${uiState.seekSeconds}秒",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
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

            // 倍速选择
            Box {
                TextButton(onClick = { showSpeedMenu = true }) {
                    Text(
                        text = "${uiState.playbackSpeed}x",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                DropdownMenu(
                    expanded = showSpeedMenu,
                    onDismissRequest = { showSpeedMenu = false },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.9f))
                ) {
                    speedOptions.forEach { speed ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${speed}x",
                                        color = if (speed == uiState.playbackSpeed)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            Color.White
                                    )
                                    if (speed == uiState.playbackSpeed) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "✓",
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onSpeedChange(speed)
                                showSpeedMenu = false
                            }
                        )
                    }
                }
            }

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

            // 更多菜单
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.9f))
                ) {
                    // 收藏/取消收藏按钮
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (uiState.isFavorite)
                                        Icons.Default.Favorite
                                    else
                                        Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (uiState.isFavorite)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.isFavorite) "取消收藏" else "收藏",
                                    color = Color.White
                                )
                            }
                        },
                        onClick = {
                            showMoreMenu = false
                            onToggleFavorite()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VideoFile,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "视频信息", color = Color.White)
                            }
                        },
                        onClick = {
                            showMoreMenu = false
                            onShowVideoInfo()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "设置", color = Color.White)
                            }
                        },
                        onClick = {
                            showMoreMenu = false
                            onShowSettings()
                        }
                    )
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

/**
 * 视频信息弹窗
 */
@Composable
private fun VideoInfoDialog(
    videoInfo: VideoInfo?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "视频信息") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                videoInfo?.let { info ->
                    InfoRow(label = "时长", value = formatTime(info.duration))
                    info.bitrate?.let { bitrate ->
                        InfoRow(label = "码率", value = "${bitrate / 1000} kbps")
                    }
                    info.videoCodec?.let { codec ->
                        InfoRow(label = "视频编码", value = codec)
                    }
                    info.audioCodec?.let { codec ->
                        InfoRow(label = "音频编码", value = codec)
                    }
                    info.resolution?.let { resolution ->
                        InfoRow(label = "分辨率", value = resolution)
                    }
                    info.frameRate?.let { fps ->
                        InfoRow(label = "帧率", value = "${fps} fps")
                    }
                    info.mimeType?.let { mime ->
                        InfoRow(label = "格式", value = mime)
                    }
                    if (info.videoUrl.isNotEmpty()) {
                        InfoRow(
                            label = "URL",
                            value = info.videoUrl,
                            maxLines = 3
                        )
                    }
                } ?: run {
                    Text(
                        text = "暂无视频信息",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "关闭")
            }
        }
    )
}

/**
 * 信息行组件
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    maxLines: Int = 1
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.3f)
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.7f)
        )
    }
}

/**
 * 播放器设置弹窗
 */
@Composable
private fun PlayerSettingsDialog(
    seekSeconds: Int,
    onSeekSecondsChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var localSeekSeconds by remember { mutableIntStateOf(seekSeconds) }
    val seekOptions = listOf(5, 10, 15, 20, 30)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "播放器设置") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 快进快退秒数设置
                Text(
                    text = "快进/快退秒数",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    seekOptions.forEach { seconds ->
                        FilterChip(
                            selected = localSeekSeconds == seconds,
                            onClick = { localSeekSeconds = seconds },
                            label = { Text(text = "${seconds}秒") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // 预留其他设置位置
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

                Text(
                    text = "更多设置选项将在后续版本中添加",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSeekSecondsChange(localSeekSeconds)
                    onDismiss()
                }
            ) {
                Text(text = "保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
}
