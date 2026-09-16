package com.tdull.webdavviewer.app.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tdull.webdavviewer.app.data.local.UploadsDataStore
import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.data.model.UploadConflictPolicy
import com.tdull.webdavviewer.app.data.model.UploadFileCandidate
import com.tdull.webdavviewer.app.data.model.UploadStatus
import com.tdull.webdavviewer.app.data.model.UploadTask
import com.tdull.webdavviewer.app.worker.UploadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 后台上传任务仓库实现。
 */
@Singleton
class UploadsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val uploadsDataStore: UploadsDataStore
) : UploadsRepository {

    private val workManager = WorkManager.getInstance(context)
    private val rescheduleMutex = Mutex()
    private var hasRescheduledQueuedUploads = false

    override val uploads: Flow<List<UploadTask>> = uploadsDataStore.getUploads()

    override suspend fun enqueueUploads(
        files: List<UploadFileCandidate>,
        serverConfig: ServerConfig,
        targetDirectory: String,
        conflictPolicies: Map<String, UploadConflictPolicy>
    ): Result<List<UploadTask>> = runCatching {
        val normalizedDirectory = normalizeDirectoryPath(targetDirectory)
        val tasks = files.map { file ->
            val taskId = java.util.UUID.randomUUID().toString()
            val cleanName = file.fileName
                .ifBlank { "未命名文件" }
                .replace("/", "_")
                .replace("\\", "_")
            val targetPath = buildChildFilePath(normalizedDirectory, cleanName)
            UploadTask(
                id = taskId,
                uriString = file.uriString,
                fileName = cleanName,
                mimeType = file.mimeType,
                fileSize = file.fileSize,
                serverId = serverConfig.id,
                serverName = serverConfig.name,
                serverUrl = serverConfig.url,
                username = serverConfig.username,
                password = serverConfig.password,
                targetDirectory = normalizedDirectory,
                targetPath = targetPath,
                tempPath = buildTemporaryPath(normalizedDirectory, cleanName, taskId),
                conflictPolicy = conflictPolicies[cleanName] ?: UploadConflictPolicy.SKIP,
                totalBytes = file.fileSize
            )
        }

        uploadsDataStore.addUploads(tasks)
        tasks.forEach { scheduleUpload(it) }
        tasks
    }

    override suspend fun getUpload(id: String): UploadTask? {
        return uploadsDataStore.getUpload(id)
    }

    override suspend fun markRunning(id: String) {
        uploadsDataStore.updateUpload(id) {
            if (it.status == UploadStatus.PAUSED ||
                it.status == UploadStatus.CANCELED ||
                it.status == UploadStatus.COMPLETED
            ) {
                it
            } else {
                it.copy(
                    status = UploadStatus.RUNNING,
                    errorMessage = null
                )
            }
        }
    }

    override suspend fun updateProgress(id: String, uploadedBytes: Long) {
        uploadsDataStore.updateUpload(id) {
            if (it.status == UploadStatus.PAUSED ||
                it.status == UploadStatus.CANCELED ||
                it.status == UploadStatus.COMPLETED
            ) {
                it
            } else {
                it.copy(
                    status = UploadStatus.RUNNING,
                    uploadedBytes = uploadedBytes
                )
            }
        }
    }

    override suspend fun markQueued(id: String, message: String?) {
        uploadsDataStore.updateUpload(id) {
            it.copy(
                status = UploadStatus.QUEUED,
                errorMessage = message
            )
        }
    }

    override suspend fun markCompleted(id: String) {
        uploadsDataStore.updateUpload(id) {
            if (it.status == UploadStatus.PAUSED || it.status == UploadStatus.CANCELED) {
                it
            } else {
                it.copy(
                    status = UploadStatus.COMPLETED,
                    uploadedBytes = if (it.totalBytes > 0L) it.totalBytes else it.uploadedBytes,
                    errorMessage = null
                )
            }
        }
    }

    override suspend fun markFailed(id: String, message: String) {
        uploadsDataStore.updateUpload(id) {
            it.copy(
                status = UploadStatus.FAILED,
                errorMessage = message
            )
        }
    }

    override suspend fun markWaitingConflict(id: String, message: String) {
        uploadsDataStore.updateUpload(id) {
            it.copy(
                status = UploadStatus.WAITING_CONFLICT,
                errorMessage = message
            )
        }
    }

    override suspend fun pauseUpload(id: String): Result<Unit> = runCatching {
        val task = uploadsDataStore.getUpload(id) ?: return@runCatching
        if (task.status == UploadStatus.COMPLETED || task.status == UploadStatus.CANCELED) {
            return@runCatching
        }
        uploadsDataStore.updateUpload(id) {
            it.copy(
                status = UploadStatus.PAUSED,
                errorMessage = "已暂停"
            )
        }
        workManager.cancelUniqueWork(task.workName)
    }

    override suspend fun continueUpload(id: String): Result<Unit> = runCatching {
        val task = uploadsDataStore.getUpload(id) ?: error("上传任务不存在")
        val continueTask = task.copy(
            status = UploadStatus.QUEUED,
            uploadedBytes = 0L,
            errorMessage = null,
            updatedAt = System.currentTimeMillis()
        )
        uploadsDataStore.updateUpload(id) { continueTask }
        scheduleUpload(continueTask)
    }

    override suspend fun cancelUpload(id: String): Result<Unit> = runCatching {
        val task = uploadsDataStore.getUpload(id) ?: return@runCatching
        uploadsDataStore.updateUpload(id) {
            it.copy(
                status = UploadStatus.CANCELED,
                errorMessage = "已取消"
            )
        }
        workManager.cancelUniqueWork(task.workName)
    }

    override suspend fun retryUpload(id: String): Result<Unit> = runCatching {
        val task = uploadsDataStore.getUpload(id) ?: error("上传任务不存在")
        val retryTask = task.copy(
            status = UploadStatus.QUEUED,
            conflictPolicy = if (task.status == UploadStatus.WAITING_CONFLICT) {
                UploadConflictPolicy.OVERWRITE
            } else {
                task.conflictPolicy
            },
            uploadedBytes = 0L,
            errorMessage = null,
            updatedAt = System.currentTimeMillis()
        )
        uploadsDataStore.updateUpload(id) { retryTask }
        scheduleUpload(retryTask)
    }

    override suspend fun rescheduleQueuedUploads(): Result<Unit> = runCatching {
        rescheduleMutex.withLock {
            if (hasRescheduledQueuedUploads) {
                return@withLock
            }

            uploadsDataStore.getUploads()
                .first()
                .filter { it.status == UploadStatus.QUEUED }
                .forEach { task ->
                    val queuedTask = task.copy(
                        status = UploadStatus.QUEUED,
                        errorMessage = task.errorMessage,
                        updatedAt = System.currentTimeMillis()
                    )
                    uploadsDataStore.updateUpload(task.id) { queuedTask }
                    scheduleUpload(queuedTask)
                }

            hasRescheduledQueuedUploads = true
        }
    }

    override suspend fun clearFinishedUploads() {
        uploadsDataStore.clearFinishedUploads()
    }

    private fun scheduleUpload(task: UploadTask) {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(UploadWorker.KEY_TASK_ID to task.id))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(task.workName, ExistingWorkPolicy.REPLACE, request)
    }

    private fun normalizeDirectoryPath(path: String): String {
        val normalized = path.ifBlank { "/" }.let { if (it.startsWith("/")) it else "/$it" }
        return if (normalized == "/") "/" else "${normalized.trimEnd('/')}/"
    }

    private fun buildChildFilePath(parentPath: String, fileName: String): String {
        return if (parentPath == "/") {
            "/$fileName"
        } else {
            "${parentPath.trimEnd('/')}/$fileName"
        }
    }

    private fun buildTemporaryPath(parentPath: String, fileName: String, taskId: String): String {
        val tempName = ".$fileName.upload-$taskId.tmp"
        return buildChildFilePath(parentPath, tempName)
    }
}
