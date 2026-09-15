package com.tdull.webdavviewer.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tdull.webdavviewer.app.data.model.BrowserLayoutMode
import com.tdull.webdavviewer.app.data.model.BrowserLayoutSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.browserLayoutSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "browser_layout_settings"
)

@Singleton
class BrowserLayoutSettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val LAYOUT_MODE_KEY = stringPreferencesKey("layout_mode")
        private val GRID_COLUMNS_KEY = intPreferencesKey("grid_columns")
    }

    fun getLayoutSettings(): Flow<BrowserLayoutSettings> {
        return context.browserLayoutSettingsDataStore.data.map { preferences ->
            val mode = preferences[LAYOUT_MODE_KEY]
                ?.let { runCatching { BrowserLayoutMode.valueOf(it) }.getOrNull() }
                ?: BrowserLayoutMode.GRID
            val columns = BrowserLayoutSettings.normalizeGridColumns(
                preferences[GRID_COLUMNS_KEY] ?: BrowserLayoutSettings.DEFAULT_GRID_COLUMNS
            )

            BrowserLayoutSettings(
                layoutMode = mode,
                gridColumns = columns
            )
        }
    }

    suspend fun saveLayoutMode(mode: BrowserLayoutMode) {
        context.browserLayoutSettingsDataStore.edit { preferences ->
            preferences[LAYOUT_MODE_KEY] = mode.name
        }
    }

    suspend fun saveGridColumns(columns: Int) {
        context.browserLayoutSettingsDataStore.edit { preferences ->
            preferences[GRID_COLUMNS_KEY] = BrowserLayoutSettings.normalizeGridColumns(columns)
        }
    }
}
