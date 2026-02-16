package com.tdull.webdavviewer.app.data.repository

import com.tdull.webdavviewer.app.data.local.ConfigDataStore
import com.tdull.webdavviewer.app.data.model.ServerConfig
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 配置仓库，封装配置存储操作
 */
@Singleton
class ConfigRepository @Inject constructor(
    private val configDataStore: ConfigDataStore
) {
    /**
     * 获取所有服务器配置
     */
    val servers: Flow<List<ServerConfig>> = configDataStore.getServerConfigs()

    /**
     * 获取当前激活的服务器配置
     */
    val activeServer: Flow<ServerConfig?> = configDataStore.getActiveServer()

    /**
     * 获取当前激活的服务器ID
     */
    val activeServerId: Flow<String?> = configDataStore.getActiveServerId()

    /**
     * 添加新服务器配置
     * @param config 服务器配置
     */
    suspend fun addServer(config: ServerConfig) {
        configDataStore.saveServerConfig(config)
    }

    /**
     * 更新服务器配置
     * @param config 更新后的服务器配置
     */
    suspend fun updateServer(config: ServerConfig) {
        configDataStore.saveServerConfig(config)
    }

    /**
     * 删除服务器配置
     * @param id 服务器ID
     */
    suspend fun removeServer(id: String) {
        configDataStore.deleteServerConfig(id)
    }

    /**
     * 设置当前激活的服务器
     * @param id 服务器ID
     */
    suspend fun setActiveServer(id: String) {
        configDataStore.setActiveServer(id)
    }

    /**
     * 清除当前激活的服务器
     */
    suspend fun clearActiveServer() {
        configDataStore.clearActiveServer()
    }
}
