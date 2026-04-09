package com.tdull.webdavviewer.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tdull.webdavviewer.app.data.model.DownloadItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.downloadsDataStore: DataStore<Preferences> by preferencesDataStore(name = "downloads")

/**
 * 使用 DataStore 存储下载记录列表
 */
@Singleton
class DownloadsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val DOWNLOADS_KEY = stringPreferencesKey("downloads_list")
    }

    /**
     * 获取下载记录列表
     */
    fun getDownloads(): Flow<List<DownloadItem>> = context.downloadsDataStore.data.map { preferences ->
        val downloadsJson = preferences[DOWNLOADS_KEY] ?: "[]"
        parseDownloadItems(downloadsJson)
    }

    /**
     * 添加下载记录
     */
    suspend fun addDownload(item: DownloadItem) {
        context.downloadsDataStore.edit { preferences ->
            val downloadsJson = preferences[DOWNLOADS_KEY] ?: "[]"
            val downloads = parseDownloadItems(downloadsJson).toMutableList()

            // 检查是否已存在相同资源路径的记录
            val existingIndex = downloads.indexOfFirst { it.resourcePath == item.resourcePath }
            if (existingIndex < 0) {
                downloads.add(0, item)  // 添加到列表开头
                preferences[DOWNLOADS_KEY] = serializeDownloadItems(downloads)
            }
        }
    }

    /**
     * 删除下载记录
     */
    suspend fun removeDownload(id: String) {
        context.downloadsDataStore.edit { preferences ->
            val downloadsJson = preferences[DOWNLOADS_KEY] ?: "[]"
            val downloads = parseDownloadItems(downloadsJson).filter { it.id != id }
            preferences[DOWNLOADS_KEY] = serializeDownloadItems(downloads)
        }
    }

    /**
     * 根据资源路径获取下载记录
     */
    suspend fun getDownloadByPath(resourcePath: String): DownloadItem? {
        val downloadsJson = context.downloadsDataStore.data.map { it[DOWNLOADS_KEY] ?: "[]" }.first()
        val downloads = parseDownloadItems(downloadsJson)
        return downloads.find { it.resourcePath == resourcePath }
    }

    /**
     * 检查是否已下载指定资源路径
     */
    fun isDownloaded(resourcePath: String): Flow<Boolean> = context.downloadsDataStore.data.map { preferences ->
        val downloadsJson = preferences[DOWNLOADS_KEY] ?: "[]"
        val downloads = parseDownloadItems(downloadsJson)
        downloads.any { it.resourcePath == resourcePath }
    }

    /**
     * 解析下载记录列表 JSON
     */
    private fun parseDownloadItems(json: String): List<DownloadItem> {
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { index ->
                val jsonObject = jsonArray.getJSONObject(index)
                DownloadItem(
                    id = jsonObject.optString("id", ""),
                    videoUrl = jsonObject.optString("videoUrl", ""),
                    videoTitle = jsonObject.optString("videoTitle", ""),
                    serverId = jsonObject.optString("serverId", ""),
                    resourcePath = jsonObject.optString("resourcePath", ""),
                    localPath = jsonObject.optString("localPath", ""),
                    fileSize = jsonObject.optLong("fileSize", 0),
                    downloadedAt = jsonObject.optLong("downloadedAt", System.currentTimeMillis())
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 序列化下载记录列表为 JSON
     */
    private fun serializeDownloadItems(items: List<DownloadItem>): String {
        val jsonArray = JSONArray()
        items.forEach { item ->
            val jsonObject = JSONObject().apply {
                put("id", item.id)
                put("videoUrl", item.videoUrl)
                put("videoTitle", item.videoTitle)
                put("serverId", item.serverId)
                put("resourcePath", item.resourcePath)
                put("localPath", item.localPath)
                put("fileSize", item.fileSize)
                put("downloadedAt", item.downloadedAt)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }
}
