package com.tdull.webdavviewer.app.data.repository

import com.tdull.webdavviewer.app.data.local.BrowserLayoutSettingsDataStore
import com.tdull.webdavviewer.app.data.model.BrowserLayoutMode
import com.tdull.webdavviewer.app.data.model.BrowserLayoutSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface BrowserLayoutSettingsRepository {
    fun getLayoutSettings(): Flow<BrowserLayoutSettings>

    suspend fun saveLayoutMode(mode: BrowserLayoutMode)

    suspend fun saveGridColumns(columns: Int)
}

class BrowserLayoutSettingsRepositoryImpl @Inject constructor(
    private val dataStore: BrowserLayoutSettingsDataStore
) : BrowserLayoutSettingsRepository {

    override fun getLayoutSettings(): Flow<BrowserLayoutSettings> {
        return dataStore.getLayoutSettings()
    }

    override suspend fun saveLayoutMode(mode: BrowserLayoutMode) {
        dataStore.saveLayoutMode(mode)
    }

    override suspend fun saveGridColumns(columns: Int) {
        dataStore.saveGridColumns(BrowserLayoutSettings.normalizeGridColumns(columns))
    }
}
