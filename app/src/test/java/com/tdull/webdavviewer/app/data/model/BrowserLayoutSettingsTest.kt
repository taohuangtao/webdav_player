package com.tdull.webdavviewer.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserLayoutSettingsTest {

    @Test
    fun `defaults to grid mode with four columns`() {
        val settings = BrowserLayoutSettings()

        assertEquals(BrowserLayoutMode.GRID, settings.layoutMode)
        assertEquals(4, settings.gridColumns)
    }

    @Test
    fun `normalizeGridColumns keeps value within bounds`() {
        assertEquals(2, BrowserLayoutSettings.normalizeGridColumns(1))
        assertEquals(4, BrowserLayoutSettings.normalizeGridColumns(4))
        assertEquals(8, BrowserLayoutSettings.normalizeGridColumns(9))
    }
}
