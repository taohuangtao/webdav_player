package com.tdull.webdavviewer.app.data.repository

import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.data.model.UploadConflictPolicy
import com.tdull.webdavviewer.app.data.model.UploadFileCandidate
import com.tdull.webdavviewer.app.data.model.UploadTask
import kotlinx.coroutines.flow.Flow

/**
 * 后台上传任务仓库。
 */
interface UploadsRepository {
    val uploads: Flow<List<UploadTask>>

    suspend fun enqueueUploads(
        files: List<UploadFileCandidate>,
        serverConfig: ServerConfig,
        targetDirectory: String,
        conflictPolicies: Map<String, UploadConflictPolicy>
    ): Result<List<UploadTask>>

    suspend fun getUpload(id: String): UploadTask?

    suspend fun markRunning(id: String)

    suspend fun updateProgress(id: String, uploadedBytes: Long)

    suspend fun markQueued(id: String, message: String? = null)

    suspend fun markCompleted(id: String)

    suspend fun markFailed(id: String, message: String)

    suspend fun markWaitingConflict(id: String, message: String)

    suspend fun pauseUpload(id: String): Result<Unit>

    suspend fun continueUpload(id: String): Result<Unit>

    suspend fun cancelUpload(id: String): Result<Unit>

    suspend fun retryUpload(id: String): Result<Unit>

    suspend fun rescheduleQueuedUploads(): Result<Unit>

    suspend fun clearFinishedUploads()
}
