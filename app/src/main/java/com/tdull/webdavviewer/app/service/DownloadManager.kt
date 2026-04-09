package com.tdull.webdavviewer.app.service

import android.content.Context
import android.os.Environment
import android.util.Log
import com.tdull.webdavviewer.app.data.model.DownloadItem
import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.data.model.WebDAVResource
import com.tdull.webdavviewer.app.data.repository.ConfigRepository
import com.tdull.webdavviewer.app.data.repository.DownloadsRepository
import com.tdull.webdavviewer.app.data.remote.WebDAVClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 下载进度信息
 */
data class DownloadProgress(
    val resourcePath: String,
    val fileName: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val isDownloading: Boolean = true,
    val isComplete: Boolean = false,
    val error: String? = null
) {
    val progress: Float
        get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()
}

/**
 * 下载管理器
 * 负责视频文件的下载、进度追踪和本地文件管理
 */
@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("WebDAV") private val okHttpClient: OkHttpClient,
    private val downloadsRepository: DownloadsRepository,
    private val configRepository: ConfigRepository,
    private val webDAVClient: WebDAVClient
) {
    companion object {
        private const val TAG = "DownloadManager"
        private const val BUFFER_SIZE = 8192
    }

    // 下载进度状态
    private val _downloadProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, DownloadProgress>> = _downloadProgress.asStateFlow()

    // 下载目录
    private val downloadDir: File
        get() = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(context.filesDir, "Movies").apply { mkdirs() }

    /**
     * 开始下载视频文件
     * @param resource WebDAV资源
     * @param serverId 服务器ID
     * @return 下载结果
     */
    suspend fun startDownload(
        resource: WebDAVResource,
        serverId: String
    ): Result<DownloadItem> = withContext(Dispatchers.IO) {
        try {
            // 检查是否已下载
            val existing = downloadsRepository.getDownloadByPath(resource.path)
            if (existing != null) {
                val localFile = File(existing.localPath)
                if (localFile.exists()) {
                    return@withContext Result.success(existing)
                } else {
                    // 本地文件已删除，清除记录
                    downloadsRepository.removeDownload(existing.id)
                }
            }

            // 获取服务器配置
            val servers = configRepository.servers.first()
            val serverConfig = servers.find { it.id == serverId }
                ?: return@withContext Result.failure(Exception("服务器配置不存在"))

            // 获取下载URL
            val downloadUrl = webDAVClient.getStreamUrl(resource.path)

            // 创建本地文件
            val localFile = createLocalFile(resource.name)
            Log.d(TAG, "开始下载: $downloadUrl -> ${localFile.absolutePath}")

            // 更新进度状态
            _downloadProgress.value = _downloadProgress.value + (
                resource.path to DownloadProgress(
                    resourcePath = resource.path,
                    fileName = resource.name,
                    totalBytes = resource.size,
                    downloadedBytes = 0,
                    isDownloading = true
                )
            )

            // 构建带认证的请求
            val request = buildDownloadRequest(downloadUrl, serverConfig)

            // 执行下载
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                _downloadProgress.value = _downloadProgress.value + (
                    resource.path to DownloadProgress(
                        resourcePath = resource.path,
                        fileName = resource.name,
                        totalBytes = resource.size,
                        downloadedBytes = 0,
                        isDownloading = false,
                        error = "下载失败: HTTP ${response.code}"
                    )
                )
                return@withContext Result.failure(Exception("下载失败: HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("响应体为空"))
            val contentLength = body.contentLength()

            // 写入文件
            var totalBytesRead = 0L
            val buffer = ByteArray(BUFFER_SIZE)

            FileOutputStream(localFile).use { output ->
                body.byteStream().use { input ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // 更新进度
                        _downloadProgress.value = _downloadProgress.value + (
                            resource.path to DownloadProgress(
                                resourcePath = resource.path,
                                fileName = resource.name,
                                totalBytes = if (contentLength > 0) contentLength else resource.size,
                                downloadedBytes = totalBytesRead,
                                isDownloading = true
                            )
                        )
                    }
                }
            }

            // 创建下载记录
            val downloadItem = DownloadItem(
                id = UUID.randomUUID().toString(),
                videoUrl = downloadUrl,
                videoTitle = resource.name,
                serverId = serverId,
                resourcePath = resource.path,
                localPath = localFile.absolutePath,
                fileSize = localFile.length(),
                downloadedAt = System.currentTimeMillis()
            )

            // 保存下载记录
            downloadsRepository.addDownload(downloadItem)

            // 更新进度为完成
            _downloadProgress.value = _downloadProgress.value + (
                resource.path to DownloadProgress(
                    resourcePath = resource.path,
                    fileName = resource.name,
                    totalBytes = localFile.length(),
                    downloadedBytes = localFile.length(),
                    isDownloading = false,
                    isComplete = true
                )
            )

            Log.d(TAG, "下载完成: ${localFile.absolutePath}")
            Result.success(downloadItem)

        } catch (e: Exception) {
            Log.e(TAG, "下载失败: ${e.message}", e)

            // 更新进度为错误
            _downloadProgress.value = _downloadProgress.value + (
                resource.path to DownloadProgress(
                    resourcePath = resource.path,
                    fileName = resource.name,
                    totalBytes = resource.size,
                    downloadedBytes = 0,
                    isDownloading = false,
                    error = e.message ?: "下载失败"
                )
            )

            Result.failure(e)
        }
    }

    /**
     * 构建带认证的下载请求
     */
    private fun buildDownloadRequest(url: String, config: ServerConfig): Request {
        val requestBuilder = Request.Builder().url(url)

        if (config.requiresAuth()) {
            val credentials = Credentials.basic(config.username, config.password)
            requestBuilder.header("Authorization", credentials)
        }

        return requestBuilder.build()
    }

    /**
     * 创建本地文件，处理文件名冲突
     */
    private fun createLocalFile(fileName: String): File {
        val dir = downloadDir
        if (!dir.exists()) {
            dir.mkdirs()
        }

        // 检查文件是否存在
        var file = File(dir, fileName)
        if (!file.exists()) {
            return file
        }

        // 文件名冲突，添加序号
        val lastDotIndex = fileName.lastIndexOf('.')
        val baseName = if (lastDotIndex > 0) fileName.substring(0, lastDotIndex) else fileName
        val extension = if (lastDotIndex > 0) fileName.substring(lastDotIndex) else ""

        var index = 1
        while (file.exists()) {
            file = File(dir, "$baseName($index)$extension")
            index++
        }

        return file
    }

    /**
     * 删除下载的文件
     * @param downloadId 下载记录ID
     */
    suspend fun deleteDownload(downloadId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val downloads = downloadsRepository.downloads.first()
            val download = downloads.find { it.id == downloadId }
                ?: return@withContext Result.failure(Exception("下载记录不存在"))

            // 删除本地文件
            val file = File(download.localPath)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "删除文件: ${file.absolutePath}, 成功: $deleted")
            }

            // 删除记录
            downloadsRepository.removeDownload(downloadId)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "删除下载失败: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 获取下载目录路径
     */
    fun getDownloadDirectory(): String {
        return downloadDir.absolutePath
    }

    /**
     * 获取下载目录可用空间
     */
    fun getAvailableSpace(): Long {
        return downloadDir.freeSpace
    }

    /**
     * 获取下载目录总空间
     */
    fun getTotalSpace(): Long {
        return downloadDir.totalSpace
    }

    /**
     * 清除下载进度
     */
    fun clearProgress(resourcePath: String) {
        _downloadProgress.value = _downloadProgress.value - resourcePath
    }
}
