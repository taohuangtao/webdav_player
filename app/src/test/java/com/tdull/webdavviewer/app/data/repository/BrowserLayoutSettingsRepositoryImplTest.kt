package com.tdull.webdavviewer.app.data.repository

import com.tdull.webdavviewer.app.data.local.BrowserLayoutSettingsDataStore
import com.tdull.webdavviewer.app.data.model.BrowserLayoutMode
import com.tdull.webdavviewer.app.data.model.BrowserLayoutSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BrowserLayoutSettingsRepositoryImplTest {

    private lateinit var dataStore: BrowserLayoutSettingsDataStore
    private lateinit var repository: BrowserLayoutSettingsRepositoryImpl

    @Before
    fun setup() {
        dataStore = mock()
        repository = BrowserLayoutSettingsRepositoryImpl(dataStore)
    }

    @Test
    fun `getLayoutSettings delegates to data store`() = runTest {
        val expected = BrowserLayoutSettings(
            layoutMode = BrowserLayoutMode.LIST,
            gridColumns = 6
        )
        whenever(dataStore.getLayoutSettings()).thenReturn(flowOf(expected))

        val result = repository.getLayoutSettings().first()

        assertEquals(expected, result)
    }

    @Test
    fun `saveLayoutMode delegates to data store`() = runTest {
        repository.saveLayoutMode(BrowserLayoutMode.LIST)

        verify(dataStore).saveLayoutMode(BrowserLayoutMode.LIST)
    }

    @Test
    fun `saveGridColumns normalizes value before delegating`() = runTest {
        repository.saveGridColumns(99)

        verify(dataStore).saveGridColumns(8)
    }
}
