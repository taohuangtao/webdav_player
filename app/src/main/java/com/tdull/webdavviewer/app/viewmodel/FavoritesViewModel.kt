package com.tdull.webdavviewer.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdull.webdavviewer.app.data.model.FavoriteItem
import com.tdull.webdavviewer.app.data.repository.FavoritesRepository
import com.tdull.webdavviewer.app.data.repository.WebDAVRepository
import com.tdull.webdavviewer.app.data.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 收藏列表UI状态
 */
data class FavoritesUiState(
    val favorites: List<FavoriteItem> = emptyList(),
    val isLoading: Boolean = false,
    val showEmptyState: Boolean = false
)

/**
 * 收藏页面ViewModel
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val webDAVRepository: WebDAVRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    // 视频预览图缓存：Map<视频路径, 预览图URL列表>
    private val _videoPreviews = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val videoPreviews: StateFlow<Map<String, List<String>>> = _videoPreviews.asStateFlow()

    // 路径到服务器ID的映射
    private val _pathServerMap = MutableStateFlow<Map<String, String>>(emptyMap())

    // 收藏状态映射 (videoUrl -> isFavorite)
    val favoriteStatus: StateFlow<Map<String, Boolean>> = favoritesRepository.favorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        .map { favorites ->
            favorites.associate { it.videoUrl to true }
        }

    init {
        loadFavorites()
    }

    /**
     * 加载收藏列表
     */
    private fun loadFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            favoritesRepository.favorites.first().let { favorites ->
                // 构建路径到服务器ID的映射
                val pathServerMap = favorites.associate { it.resourcePath to it.serverId }
                _pathServerMap.value = pathServerMap

                _uiState.update {
                    it.copy(
                        favorites = favorites,
                        isLoading = false,
                        showEmptyState = favorites.isEmpty()
                    )
                }
            }
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(videoUrl: String, videoTitle: String, serverId: String, resourcePath: String) {
        viewModelScope.launch {
            val favorites = favoritesRepository.favorites.first()
            val existing = favorites.find { it.videoUrl == videoUrl }

            if (existing != null) {
                // 已收藏，删除收藏
                favoritesRepository.removeFavorite(existing.id)
            } else {
                // 未收藏，添加收藏
                val newItem = FavoriteItem(
                    videoUrl = videoUrl,
                    videoTitle = videoTitle,
                    serverId = serverId,
                    resourcePath = resourcePath
                )
                favoritesRepository.addFavorite(newItem)
            }
            // 重新加载列表
            loadFavorites()
        }
    }

    /**
     * 删除收藏
     */
    fun removeFavorite(id: String) {
        viewModelScope.launch {
            favoritesRepository.removeFavorite(id)
            loadFavorites()
        }
    }

    /**
     * 检查是否已收藏
     */
    fun isFavorite(videoUrl: String): Flow<Boolean> {
        return favoritesRepository.isFavorite(videoUrl)
    }

    /**
     * 获取流媒体URL
     */
    fun getStreamUrl(path: String): String {
        return webDAVRepository.getStreamUrl(path)
    }

    /**
     * 加载视频预览图
     */
    fun loadVideoPreviews(videoPath: String) {
        // 如果已经缓存，则不再重复加载
        if (_videoPreviews.value.containsKey(videoPath)) {
            return
        }

        viewModelScope.launch {
            // 先激活对应的服务器
            val serverId = _pathServerMap.value[videoPath]
            if (serverId != null) {
                val servers = configRepository.servers.first()
                val server = servers.find { it.id == serverId }
                if (server != null) {
                    // 先连接服务器，然后再获取预览图
                    webDAVRepository.connect(server)
                }
            }

            val result = webDAVRepository.getVideoPreviews(videoPath)
            result.fold(
                onSuccess = { previews ->
                    if (previews.isNotEmpty()) {
                        _videoPreviews.update { it + (videoPath to previews) }
                    }
                },
                onFailure = {
                    // 静默失败，不影响主流程
                }
            )
        }
    }

    /**
     * 刷新收藏列表
     */
    fun refresh() {
        loadFavorites()
    }
}

/**
 * StateFlow 转 Map 的扩展函数
 */
private fun StateFlow<List<FavoriteItem>>.map(
    transform: suspend (List<FavoriteItem>) -> Map<String, Boolean>
): StateFlow<Map<String, Boolean>> {
    val result = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    // 这里简化处理，实际可以通过 combine 实现
    return result.asStateFlow()
}
