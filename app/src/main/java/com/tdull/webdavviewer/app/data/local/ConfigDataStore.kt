package com.tdull.webdavviewer.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tdull.webdavviewer.app.data.model.ServerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "webdav_config")

/**
 * 使用DataStore存储WebDAV服务器配置
 */
@Singleton
class ConfigDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val SERVERS_KEY = stringPreferencesKey("servers")
        private val ACTIVE_SERVER_KEY = stringPreferencesKey("active_server_id")
    }

    /**
     * 获取所有服务器配置
     */
    fun getServerConfigs(): Flow<List<ServerConfig>> = context.dataStore.data.map { preferences ->
        val serversJson = preferences[SERVERS_KEY] ?: "[]"
        parseServerConfigs(serversJson)
    }

    /**
     * 获取当前激活的服务器配置
     */
    fun getActiveServer(): Flow<ServerConfig?> = context.dataStore.data.map { preferences ->
        val activeServerId = preferences[ACTIVE_SERVER_KEY] ?: return@map null
        val serversJson = preferences[SERVERS_KEY] ?: "[]"
        val servers = parseServerConfigs(serversJson)
        servers.find { it.id == activeServerId }
    }

    /**
     * 获取当前激活的服务器ID
     */
    fun getActiveServerId(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACTIVE_SERVER_KEY]
    }

    /**
     * 保存服务器配置（新增或更新）
     */
    suspend fun saveServerConfig(config: ServerConfig) {
        context.dataStore.edit { preferences ->
            val serversJson = preferences[SERVERS_KEY] ?: "[]"
            val servers = parseServerConfigs(serversJson).toMutableList()
            
            // 查找是否存在相同ID的配置
            val existingIndex = servers.indexOfFirst { it.id == config.id }
            if (existingIndex >= 0) {
                servers[existingIndex] = config
            } else {
                servers.add(config)
            }
            
            preferences[SERVERS_KEY] = serializeServerConfigs(servers)
        }
    }

    /**
     * 删除服务器配置
     */
    suspend fun deleteServerConfig(id: String) {
        context.dataStore.edit { preferences ->
            val serversJson = preferences[SERVERS_KEY] ?: "[]"
            val servers = parseServerConfigs(serversJson).filter { it.id != id }
            preferences[SERVERS_KEY] = serializeServerConfigs(servers)
            
            // 如果删除的是当前激活的服务器，清除激活状态
            if (preferences[ACTIVE_SERVER_KEY] == id) {
                preferences.remove(ACTIVE_SERVER_KEY)
            }
        }
    }

    /**
     * 设置当前激活的服务器
     */
    suspend fun setActiveServer(id: String) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_SERVER_KEY] = id
        }
    }

    /**
     * 清除当前激活的服务器
     */
    suspend fun clearActiveServer() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACTIVE_SERVER_KEY)
        }
    }

    /**
     * 解析服务器配置JSON
     */
    private fun parseServerConfigs(json: String): List<ServerConfig> {
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { index ->
                val jsonObject = jsonArray.getJSONObject(index)
                ServerConfig(
                    id = jsonObject.optString("id", UUID.randomUUID().toString()),
                    name = jsonObject.optString("name", ""),
                    url = jsonObject.optString("url", ""),
                    username = jsonObject.optString("username", ""),
                    password = jsonObject.optString("password", "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 序列化服务器配置为JSON
     */
    private fun serializeServerConfigs(configs: List<ServerConfig>): String {
        val jsonArray = JSONArray()
        configs.forEach { config ->
            val jsonObject = JSONObject().apply {
                put("id", config.id)
                put("name", config.name)
                put("url", config.url)
                put("username", config.username)
                put("password", config.password)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }
}
