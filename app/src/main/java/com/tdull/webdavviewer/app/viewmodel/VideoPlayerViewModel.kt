package com.tdull.webdavviewer.app.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.tdull.webdavviewer.app.data.remote.WebDAVClient
import com.tdull.webdavviewer.app.util.ErrorHandler
import com.tdull.webdavviewer.app.util.ErrorInfo
import com.tdull.webdavviewer.app.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.Credentials
import javax.inject.Inject

/**
 * 视频播放器UI状态
 */
data class VideoPlayerUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val errorInfo: ErrorInfo? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val isNetworkAvailable: Boolean = true,
    val volume: Float = 1f, // 音量 0-1
    val isPlaybackEnded: Boolean = false // 是否播放结束
)

/**
 * 视频播放器ViewModel
 * 管理 ExoPlayer 的生命周期和状态
 */
@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val networkMonitor: NetworkMonitor,
    private val webDAVClient: WebDAVClient  // 注入 WebDAVClient 用于获取认证信息
) : ViewModel() {

    private val _player = MutableStateFlow<ExoPlayer?>(null)
    val player: StateFlow<ExoPlayer?> = _player.asStateFlow()

    private val _playWhenReady = MutableStateFlow(true)
    val playWhenReady: StateFlow<Boolean> = _playWhenReady.asStateFlow()

    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    // 当前播放的URL
    private var currentVideoUrl: String? = null

    // 播放器事件监听器
    private var playerListener: Player.Listener? = null

    // 进度更新任务
    private var progressUpdateJob: Job? = null

    // 音频管理器
    private val audioManager: AudioManager by lazy {
        application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    init {
        // 监听网络状态
        viewModelScope.launch {
            networkMonitor.networkStatus.collect { status ->
                _uiState.update { it.copy(isNetworkAvailable = status.isAvailable) }
            }
        }
    }

    /**
     * 初始化播放器
     */
    fun initializePlayer(url: String) {
        // 如果URL相同且播放器已存在，则不需要重新初始化
        if (url == currentVideoUrl && _player.value != null) {
            return
        }

        // 释放之前的播放器
        releasePlayer()

        currentVideoUrl = url

        // 检查网络状态
        if (!networkMonitor.isNetworkAvailable()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorInfo = ErrorInfo(
                        type = com.tdull.webdavviewer.app.util.ErrorType.NETWORK_UNAVAILABLE,
                        title = "无网络连接",
                        message = "请检查您的网络连接后重试",
                        canRetry = true
                    ),
                    error = "无网络连接"
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, errorInfo = null) }

                // 创建带有认证的 HttpDataSource.Factory
                val dataSourceFactory = createHttpDataSourceFactory()
                
                // 创建 ExoPlayer 实例，配置认证数据源
                val exoPlayer = ExoPlayer.Builder(application)
                    .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                    .build()
                    .apply {
                        // 创建媒体项
                        val mediaItem = MediaItem.fromUri(url)
                        setMediaItem(mediaItem)
                        
                        // 设置播放器事件监听
                        playerListener = createPlayerListener()
                        addListener(playerListener!!)
                        
                        // 准备播放
                        prepare()
                        
                        // 设置播放状态
                        playWhenReady = _playWhenReady.value
                    }

                _player.value = exoPlayer
                _uiState.update { it.copy(isLoading = false) }
                startProgressUpdate() // 启动进度更新
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.getErrorInfo(e, application)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorInfo = errorInfo,
                        error = errorInfo.message
                    )
                }
            }
        }
    }

    /**
     * 创建播放器事件监听器
     */
    private fun createPlayerListener(): Player.Listener {
        return object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    ExoPlayer.STATE_READY -> {
                        _uiState.update { it.copy(isLoading = false, error = null, errorInfo = null) }
                    }
                    ExoPlayer.STATE_BUFFERING -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    ExoPlayer.STATE_ENDED -> {
                        _uiState.update { it.copy(isPlaying = false, isPlaybackEnded = true) }
                    }
                    ExoPlayer.STATE_IDLE -> {
                        // 播放器空闲
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlayerError(error: PlaybackException) {
                val errorInfo = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> {
                        ErrorInfo(
                            type = com.tdull.webdavviewer.app.util.ErrorType.NETWORK_UNAVAILABLE,
                            title = "网络错误",
                            message = "网络连接失败，请检查网络设置后重试",
                            canRetry = true
                        )
                    }
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                        ErrorInfo(
                            type = com.tdull.webdavviewer.app.util.ErrorType.SERVER_ERROR,
                            title = "服务器错误",
                            message = "服务器返回错误，请稍后重试",
                            canRetry = true
                        )
                    }
                    PlaybackException.ERROR_CODE_DECODING_FAILED,
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> {
                        ErrorInfo(
                            type = com.tdull.webdavviewer.app.util.ErrorType.UNSUPPORTED_FORMAT,
                            title = "播放失败",
                            message = "不支持的视频格式或解码失败",
                            canRetry = false
                        )
                    }
                    else -> {
                        ErrorInfo(
                            type = com.tdull.webdavviewer.app.util.ErrorType.UNKNOWN,
                            title = "播放出错",
                            message = error.message ?: "未知错误",
                            canRetry = true
                        )
                    }
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorInfo = errorInfo,
                        error = errorInfo.message
                    )
                }
            }
        }
    }

    /**
     * 播放/暂停
     */
    fun togglePlayPause() {
        _player.value?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
            _playWhenReady.value = player.playWhenReady
        }
    }

    /**
     * 播放
     */
    fun play() {
        _player.value?.play()
        _playWhenReady.value = true
    }

    /**
     * 暂停
     */
    fun pause() {
        _player.value?.pause()
        _playWhenReady.value = false
    }

    /**
     * 跳转到指定位置
     */
    fun seekTo(positionMs: Long) {
        _player.value?.seekTo(positionMs)
    }

    /**
     * 重新播放（从头开始）
     */
    fun replay() {
        _player.value?.let { player ->
            player.seekTo(0)
            player.play()
            _uiState.update { it.copy(isPlaybackEnded = false) }
            _playWhenReady.value = true
        }
    }

    /**
     * 设置音量
     * @param volume 音量值 0-1
     */
    fun setVolume(volume: Float) {
        _player.value?.volume = volume
        _uiState.update { it.copy(volume = volume) }
    }

    /**
     * 获取当前音量
     */
    fun getVolume(): Float {
        return _player.value?.volume ?: 1f
    }

    /**
     * 释放播放器
     */
    fun releasePlayer() {
        stopProgressUpdate() // 停止进度更新
        _player.value?.let { player ->
            // 移除监听器
            playerListener?.let { player.removeListener(it) }
            
            // 保存播放状态
            _playWhenReady.value = player.playWhenReady
            
            // 释放播放器
            player.release()
        }
        _player.value = null
        playerListener = null
        currentVideoUrl = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }

    /**
     * 重试播放
     */
    fun retry() {
        currentVideoUrl?.let { url ->
            initializePlayer(url)
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null, errorInfo = null) }
    }

    /**
     * 开始更新播放进度
     */
    private fun startProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (true) {
                _player.value?.let { player ->
                    _uiState.update {
                        it.copy(
                            currentPosition = player.currentPosition,
                            duration = player.duration.coerceAtLeast(0L),
                            volume = player.volume
                        )
                    }
                }
                delay(500) // 每500ms更新一次
            }
        }
    }

    /**
     * 停止更新播放进度
     */
    private fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }
    
    /**
     * 创建带有 WebDAV 认证的 HttpDataSource.Factory
     * 自动为请求添加 Authorization 头部
     */
    private fun createHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
        val factory = DefaultHttpDataSource.Factory()
            .setUserAgent("WebDAVViewer")
            .setAllowCrossProtocolRedirects(true)
        
        // 如果有服务器配置且需要认证，添加认证头部
        webDAVClient.getCurrentConfig()?.let { config ->
            if (config.requiresAuth()) {
                val credentials = Credentials.basic(config.username, config.password)
                factory.setDefaultRequestProperties(mapOf("Authorization" to credentials))
            }
        }
        
        return factory
    }
}
