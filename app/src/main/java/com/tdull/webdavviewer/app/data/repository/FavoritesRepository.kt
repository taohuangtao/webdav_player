package com.tdull.webdavviewer.app.data.repository

import com.tdull.webdavviewer.app.data.model.FavoriteItem
import kotlinx.coroutines.flow.Flow

/**
 * 收藏仓库接口
 */
interface FavoritesRepository {
    /**
     * 收藏列表流
     */
    val favorites: Flow<List<FavoriteItem>>

    /**
     * 添加收藏
     */
    suspend fun addFavorite(item: FavoriteItem)

    /**
     * 删除收藏
     */
    suspend fun removeFavorite(id: String)

    /**
     * 检查指定 URL 是否已收藏
     */
    fun isFavorite(videoUrl: String): Flow<Boolean>
}
