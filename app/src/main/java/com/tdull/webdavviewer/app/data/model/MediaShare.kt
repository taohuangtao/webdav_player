package com.tdull.webdavviewer.app.data.model

import android.net.Uri

data class MediaShareRequest(
    val url: String,
    val title: String,
    val mimeType: String? = null
)

data class MediaSharePayload(
    val uri: Uri,
    val title: String,
    val mimeType: String
)

data class MediaShareUiState(
    val isPreparing: Boolean = false,
    val progressPercent: Int? = null,
    val error: String? = null,
    val payload: MediaSharePayload? = null
)
