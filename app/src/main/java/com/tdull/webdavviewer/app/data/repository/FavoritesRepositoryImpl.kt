package com.tdull.webdavviewer.app.data.repository

import com.tdull.webdavviewer.app.data.local.FavoritesDataStore
import com.tdull.webdavviewer.app.data.model.FavoriteItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 收藏仓库实现
 */
@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val favoritesDataStore: FavoritesDataStore
) : FavoritesRepository {

    override val favorites: Flow<List<FavoriteItem>>
        get() = favoritesDataStore.getFavorites()

    override suspend fun addFavorite(item: FavoriteItem) {
        favoritesDataStore.addFavorite(item)
    }

    override suspend fun removeFavorite(id: String) {
        favoritesDataStore.removeFavorite(id)
    }

    override fun isFavorite(videoUrl: String): Flow<Boolean> {
        return favoritesDataStore.isFavorite(videoUrl)
    }
}
