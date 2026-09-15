package com.tdull.webdavviewer.app.data.model

enum class BrowserLayoutMode {
    GRID,
    LIST
}

data class BrowserLayoutSettings(
    val layoutMode: BrowserLayoutMode = BrowserLayoutMode.GRID,
    val gridColumns: Int = DEFAULT_GRID_COLUMNS
) {
    companion object {
        const val MIN_GRID_COLUMNS = 2
        const val MAX_GRID_COLUMNS = 8
        const val DEFAULT_GRID_COLUMNS = 4

        fun normalizeGridColumns(columns: Int): Int {
            return columns.coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS)
        }
    }
}
