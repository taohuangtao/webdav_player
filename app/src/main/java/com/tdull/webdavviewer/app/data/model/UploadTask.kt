package com.tdull.webdavviewer.app.data.model

import java.util.UUID

/**
 * 上传任务状态。
 */
enum class UploadStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    WAITING_CONFLICT,
    COMPLETED,
    FAILED,
    CANCELED
}

/**
 * 远端同名文件处理策略。
 */
enum class UploadConflictPolicy {
    SKIP,
    OVERWRITE
}

/**
 * 后台上传任务。
 *
 * 任务保存服务器连接快照，避免用户切换当前服务器后把文件上传到错误位置。
 */
data class UploadTask(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val fileName: String,
    val mimeType: String? = null,
    val fileSize: Long = -1L,
    val serverId: String,
    val serverName: String,
    val serverUrl: String,
    val username: String = "",
    val password: String = "",
    val targetDirectory: String,
    val targetPath: String,
    val tempPath: String,
    val conflictPolicy: UploadConflictPolicy = UploadConflictPolicy.SKIP,
    val status: UploadStatus = UploadStatus.QUEUED,
    val uploadedBytes: Long = 0L,
    val totalBytes: Long = fileSize,
    val errorMessage: String? = null,
    val workName: String = "upload_task_$id",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val progressPercent: Int
        get() = if (totalBytes > 0L) {
            ((uploadedBytes.coerceAtMost(totalBytes) * 100L) / totalBytes).toInt()
        } else {
            0
        }

    val isFinished: Boolean
        get() = status == UploadStatus.COMPLETED ||
            status == UploadStatus.FAILED ||
            status == UploadStatus.CANCELED ||
            status == UploadStatus.WAITING_CONFLICT
}

/**
 * 文件选择器返回后，用于准备上传的轻量文件信息。
 */
data class UploadFileCandidate(
    val uriString: String,
    val fileName: String,
    val mimeType: String? = null,
    val fileSize: Long = -1L
)
