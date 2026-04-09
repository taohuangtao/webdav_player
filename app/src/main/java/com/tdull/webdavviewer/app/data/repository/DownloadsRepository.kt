package com.tdull.webdavviewer.app.data.repository

import com.tdull.webdavviewer.app.data.model.DownloadItem
import kotlinx.coroutines.flow.Flow

/**
 * 下载记录仓库接口
 */
interface DownloadsRepository {
    /**
     * 获取所有下载记录
     */
    val downloads: Flow<List<DownloadItem>>

    /**
     * 添加下载记录
     */
    suspend fun addDownload(item: DownloadItem)

    /**
     * 删除下载记录
     */
    suspend fun removeDownload(id: String)

    /**
     * 根据资源路径获取下载记录
     */
    suspend fun getDownloadByPath(resourcePath: String): DownloadItem?

    /**
     * 检查是否已下载
     */
    fun isDownloaded(resourcePath: String): Flow<Boolean>
}
