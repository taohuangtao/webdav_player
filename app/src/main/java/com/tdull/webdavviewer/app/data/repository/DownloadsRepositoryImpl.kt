package com.tdull.webdavviewer.app.data.repository

import com.tdull.webdavviewer.app.data.local.DownloadsDataStore
import com.tdull.webdavviewer.app.data.model.DownloadItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 下载记录仓库实现
 */
@Singleton
class DownloadsRepositoryImpl @Inject constructor(
    private val downloadsDataStore: DownloadsDataStore
) : DownloadsRepository {

    override val downloads: Flow<List<DownloadItem>> = downloadsDataStore.getDownloads()

    override suspend fun addDownload(item: DownloadItem) {
        downloadsDataStore.addDownload(item)
    }

    override suspend fun removeDownload(id: String) {
        downloadsDataStore.removeDownload(id)
    }

    override suspend fun getDownloadByPath(resourcePath: String): DownloadItem? {
        return downloadsDataStore.getDownloadByPath(resourcePath)
    }

    override fun isDownloaded(resourcePath: String): Flow<Boolean> {
        return downloadsDataStore.isDownloaded(resourcePath)
    }
}
