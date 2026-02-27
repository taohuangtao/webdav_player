package com.tdull.webdavviewer.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tdull.webdavviewer.app.data.model.FavoriteItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

/**
 * 使用 DataStore 存储收藏列表
 */
@Singleton
class FavoritesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val FAVORITES_KEY = stringPreferencesKey("favorites_list")
    }

    /**
     * 获取收藏列表
     */
    fun getFavorites(): Flow<List<FavoriteItem>> = context.favoritesDataStore.data.map { preferences ->
        val favoritesJson = preferences[FAVORITES_KEY] ?: "[]"
        parseFavoriteItems(favoritesJson)
    }

    /**
     * 添加收藏
     */
    suspend fun addFavorite(item: FavoriteItem) {
        context.favoritesDataStore.edit { preferences ->
            val favoritesJson = preferences[FAVORITES_KEY] ?: "[]"
            val favorites = parseFavoriteItems(favoritesJson).toMutableList()

            // 检查是否已存在相同 URL 的收藏
            val existingIndex = favorites.indexOfFirst { it.videoUrl == item.videoUrl }
            if (existingIndex < 0) {
                favorites.add(0, item)  // 添加到列表开头
                preferences[FAVORITES_KEY] = serializeFavoriteItems(favorites)
            }
        }
    }

    /**
     * 删除收藏
     */
    suspend fun removeFavorite(id: String) {
        context.favoritesDataStore.edit { preferences ->
            val favoritesJson = preferences[FAVORITES_KEY] ?: "[]"
            val favorites = parseFavoriteItems(favoritesJson).filter { it.id != id }
            preferences[FAVORITES_KEY] = serializeFavoriteItems(favorites)
        }
    }

    /**
     * 检查是否已收藏指定 URL
     */
    fun isFavorite(videoUrl: String): Flow<Boolean> = context.favoritesDataStore.data.map { preferences ->
        val favoritesJson = preferences[FAVORITES_KEY] ?: "[]"
        val favorites = parseFavoriteItems(favoritesJson)
        favorites.any { it.videoUrl == videoUrl }
    }

    /**
     * 解析收藏列表 JSON
     */
    private fun parseFavoriteItems(json: String): List<FavoriteItem> {
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { index ->
                val jsonObject = jsonArray.getJSONObject(index)
                FavoriteItem(
                    id = jsonObject.optString("id", ""),
                    videoUrl = jsonObject.optString("videoUrl", ""),
                    videoTitle = jsonObject.optString("videoTitle", ""),
                    serverId = jsonObject.optString("serverId", ""),
                    resourcePath = jsonObject.optString("resourcePath", ""),
                    addedAt = jsonObject.optLong("addedAt", System.currentTimeMillis())
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 序列化收藏列表为 JSON
     */
    private fun serializeFavoriteItems(items: List<FavoriteItem>): String {
        val jsonArray = JSONArray()
        items.forEach { item ->
            val jsonObject = JSONObject().apply {
                put("id", item.id)
                put("videoUrl", item.videoUrl)
                put("videoTitle", item.videoTitle)
                put("serverId", item.serverId)
                put("resourcePath", item.resourcePath)
                put("addedAt", item.addedAt)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }
}
