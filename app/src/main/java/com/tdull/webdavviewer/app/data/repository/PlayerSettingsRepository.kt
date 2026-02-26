package com.tdull.webdavviewer.app.data.repository

import com.tdull.webdavviewer.app.data.local.PlayerSettingsDataStore
import com.tdull.webdavviewer.app.data.model.PlayerSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 播放器设置仓库接口
 */
interface PlayerSettingsRepository {
    /**
     * 获取播放器设置
     */
    fun getPlayerSettings(): Flow<PlayerSettings>

    /**
     * 保存快进快退秒数
     */
    suspend fun saveSeekSeconds(seconds: Int)

    /**
     * 保存播放速度
     */
    suspend fun savePlaybackSpeed(speed: Float)

    /**
     * 保存所有设置
     */
    suspend fun savePlayerSettings(settings: PlayerSettings)
}

/**
 * 播放器设置仓库实现
 */
class PlayerSettingsRepositoryImpl @Inject constructor(
    private val dataStore: PlayerSettingsDataStore
) : PlayerSettingsRepository {

    override fun getPlayerSettings(): Flow<PlayerSettings> = dataStore.getPlayerSettings()

    override suspend fun saveSeekSeconds(seconds: Int) {
        dataStore.saveSeekSeconds(seconds)
    }

    override suspend fun savePlaybackSpeed(speed: Float) {
        dataStore.savePlaybackSpeed(speed)
    }

    override suspend fun savePlayerSettings(settings: PlayerSettings) {
        dataStore.savePlayerSettings(settings)
    }
}
