package com.tdull.webdavviewer.app.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.tdull.webdavviewer.app.data.model.UploadStatus
import com.tdull.webdavviewer.app.data.model.UploadTask
import com.tdull.webdavviewer.app.data.model.WebDAVException
import com.tdull.webdavviewer.app.data.remote.UploadConflictException
import com.tdull.webdavviewer.app.data.remote.WebDAVUploadClient
import com.tdull.webdavviewer.app.data.repository.UploadsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 单文件后台上传任务。
 */
@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val uploadsRepository: UploadsRepository,
    private val uploadClient: WebDAVUploadClient
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_UPLOADED_BYTES = "uploaded_bytes"

        private const val TAG = "UploadWorker"
        private const val MAX_RETRY_COUNT = 3
        private const val PROGRESS_UPDATE_INTERVAL_MS = 500L

        private val uploadMutex = Mutex()
    }

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.success()
        val task = uploadsRepository.getUpload(taskId) ?: return Result.success()

        if (task.status == UploadStatus.PAUSED ||
            task.status == UploadStatus.CANCELED ||
            task.status == UploadStatus.COMPLETED
        ) {
            return Result.success()
        }

        return uploadMutex.withLock {
            runUpload(taskId, task)
        }
    }

    private suspend fun runUpload(
        taskId: String,
        task: UploadTask
    ): Result {
        if (shouldKeepCurrentState(taskId)) {
            return Result.success()
        }

        uploadsRepository.markRunning(taskId)

        var lastProgressUpdate = 0L

        return try {
            val uri = Uri.parse(task.uriString)
            uploadClient.uploadFile(
                task = task,
                openInputStream = {
                    applicationContext.contentResolver.openInputStream(uri)
                        ?: error("无法打开本地文件")
                },
                onProgress = { uploadedBytes ->
                    val now = System.currentTimeMillis()
                    if (now - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL_MS) {
                        lastProgressUpdate = now
                        setProgressAsync(workDataOf(KEY_UPLOADED_BYTES to uploadedBytes))
                        runBlocking {
                            uploadsRepository.updateProgress(taskId, uploadedBytes)
                        }
                    }
                },
                shouldCancel = { isStopped }
            )

            if (shouldKeepCurrentState(taskId)) {
                return Result.success()
            }
            uploadsRepository.markCompleted(taskId)
            Result.success()
        } catch (e: UploadConflictException) {
            if (shouldKeepCurrentState(taskId)) {
                return Result.success()
            }
            Log.w(TAG, "Upload conflict for task $taskId", e)
            uploadsRepository.markWaitingConflict(taskId, e.message ?: "目标文件已存在")
            Result.success()
        } catch (e: WebDAVException) {
            if (shouldKeepCurrentState(taskId)) {
                return Result.success()
            }
            Log.w(TAG, "Upload failed for task $taskId", e)
            handleUploadFailure(taskId, e.message ?: "上传失败", retryable = e.isRetryable())
        } catch (e: Exception) {
            if (shouldKeepCurrentState(taskId)) {
                return Result.success()
            }
            Log.w(TAG, "Upload failed for task $taskId", e)
            val message = e.message ?: "上传失败"
            handleUploadFailure(taskId, message, retryable = true)
        }
    }

    private suspend fun handleUploadFailure(
        taskId: String,
        message: String,
        retryable: Boolean
    ): Result {
        if (shouldKeepCurrentState(taskId)) {
            return Result.success()
        }

        return if (retryable && runAttemptCount < MAX_RETRY_COUNT) {
            uploadsRepository.markQueued(taskId, "等待重试：$message")
            Result.retry()
        } else {
            uploadsRepository.markFailed(taskId, message)
            Result.success()
        }
    }

    private fun WebDAVException.isRetryable(): Boolean {
        return when (this) {
            is WebDAVException.ConnectionFailed,
            is WebDAVException.Timeout -> true
            is WebDAVException.ServerError -> statusCode in 500..599
            else -> false
        }
    }

    private suspend fun shouldKeepCurrentState(taskId: String): Boolean {
        val status = uploadsRepository.getUpload(taskId)?.status ?: return true
        return status == UploadStatus.PAUSED ||
            status == UploadStatus.CANCELED ||
            status == UploadStatus.COMPLETED ||
            (isStopped && status == UploadStatus.QUEUED)
    }
}
