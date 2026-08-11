package com.tdull.webdavviewer.app.ui.favorites

import com.tdull.webdavviewer.app.data.model.FavoriteItem
import com.tdull.webdavviewer.app.data.model.ResourceType
import com.tdull.webdavviewer.app.data.model.WebDAVResource

/**
 * 将收藏项映射为 WebDAVResource，以便复用 FileItem 紧凑列表样式。
 * 收藏项均为视频。
 */
fun FavoriteItem.toWebDAVResource(): WebDAVResource {
    return WebDAVResource(
        path = resourcePath,
        name = videoTitle.ifEmpty { "未命名视频" },
        isDirectory = false,
        size = 0,
        lastModified = addedAt,
        contentType = null,
        resourceType = ResourceType.VIDEO
    )
}
