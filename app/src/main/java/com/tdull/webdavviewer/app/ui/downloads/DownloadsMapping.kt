package com.tdull.webdavviewer.app.ui.downloads

import com.tdull.webdavviewer.app.data.model.DownloadItem
import com.tdull.webdavviewer.app.data.model.ResourceType
import com.tdull.webdavviewer.app.data.model.WebDAVResource

/**
 * 将已下载项映射为 WebDAVResource，以便复用 FileItem 紧凑列表样式。
 * 已下载项均为视频。
 */
fun DownloadItem.toWebDAVResource(): WebDAVResource {
    return WebDAVResource(
        path = resourcePath,
        name = videoTitle.ifEmpty { "未命名视频" },
        isDirectory = false,
        size = fileSize,
        lastModified = downloadedAt,
        contentType = null,
        resourceType = ResourceType.VIDEO
    )
}
