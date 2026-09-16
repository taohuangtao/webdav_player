package com.tdull.webdavviewer.app.data.remote

import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.data.model.UploadTask
import com.tdull.webdavviewer.app.data.model.WebDAVException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UploadConflictException(message: String) : Exception(message)

/**
 * 用于后台上传的 WebDAV 客户端。
 *
 * 它不依赖 WebDAVClient 的 currentConfig，避免后台任务被前台浏览器切换服务器影响。
 */
@Singleton
class WebDAVUploadClient @Inject constructor(
    @Named("WebDAV") private val okHttpClient: OkHttpClient
) {
    suspend fun uploadFile(
        task: UploadTask,
        openInputStream: () -> InputStream,
        onProgress: (Long) -> Unit,
        shouldCancel: () -> Boolean
    ) {
        val config = task.toServerConfig()
        val targetUrl = buildResourceUrl(config, task.targetPath, isDirectory = false)

        val requestBody = StreamingUploadBody(
            mimeType = task.mimeType,
            contentLength = task.fileSize,
            openInputStream = openInputStream,
            onProgress = onProgress,
            shouldCancel = shouldCancel
        )

        val putRequest = Request.Builder()
            .url(targetUrl)
            .put(requestBody)
            .applyAuth(config)
            .build()

        executeWriteRequest(putRequest)
    }

    suspend fun deleteTemporaryFile(task: UploadTask) {
        val config = task.toServerConfig()
        val request = Request.Builder()
            .url(buildResourceUrl(config, task.tempPath, isDirectory = false))
            .method("DELETE", null)
            .applyAuth(config)
            .build()

        try {
            executeWriteRequest(request)
        } catch (e: WebDAVException.ResourceNotFound) {
            // 已经不存在即可视为清理完成。
        }
    }

    private suspend fun executeWriteRequest(request: Request) {
        val response = try {
            okHttpClient.newCall(request).await()
        } catch (e: IOException) {
            throw WebDAVException.ConnectionFailed(e)
        }

        response.use {
            if (!it.isSuccessful) {
                handleWriteErrorResponse(it)
            }
        }
    }

    private fun handleWriteErrorResponse(response: Response): Nothing {
        when (response.code) {
            401 -> throw WebDAVException.AuthenticationFailed()
            403 -> throw WebDAVException.OperationFailed("没有权限上传文件，可能被服务器拒绝")
            404 -> throw WebDAVException.ResourceNotFound("上传目标")
            405 -> throw WebDAVException.UnsupportedOperation()
            409 -> throw WebDAVException.OperationFailed("上传失败：父目录不存在或路径冲突")
            412 -> throw UploadConflictException("目标文件已存在")
            507 -> throw WebDAVException.OperationFailed("服务器存储空间不足，上传失败")
            in 500..599 -> throw WebDAVException.ServerError(
                response.code,
                "${response.request.method} ${response.request.url.encodedPath}: ${response.message}"
            )
            else -> throw WebDAVException.OperationFailed("上传失败 (${response.code}): ${response.message}")
        }
    }

    private fun UploadTask.toServerConfig(): ServerConfig {
        return ServerConfig(
            id = serverId,
            name = serverName,
            url = serverUrl,
            username = username,
            password = password
        )
    }

    private fun Request.Builder.applyAuth(config: ServerConfig): Request.Builder {
        if (config.requiresAuth()) {
            header("Authorization", Credentials.basic(config.username, config.password))
        }
        return this
    }

    private fun buildResourceUrl(config: ServerConfig, path: String, isDirectory: Boolean): String {
        val baseUrl = config.getNormalizedUrl()
        val normalizedPath = if (path.startsWith("/")) path.substring(1) else path
        val encoded = normalizedPath.encodePath()
        val shouldEndWithSlash = isDirectory && normalizedPath.isNotEmpty() && !normalizedPath.endsWith("/")
        return if (shouldEndWithSlash) {
            "$baseUrl$encoded/"
        } else {
            "$baseUrl$encoded"
        }
    }

    private fun String.encodePath(): String {
        return split("/").joinToString("/") { segment ->
            URLEncoder.encode(segment, StandardCharsets.UTF_8.name())
                .replace("+", "%20")
                .replace("%2F", "/")
        }
    }
}

private class StreamingUploadBody(
    private val mimeType: String?,
    private val contentLength: Long,
    private val openInputStream: () -> InputStream,
    private val onProgress: (Long) -> Unit,
    private val shouldCancel: () -> Boolean
) : RequestBody() {
    override fun contentType() = (mimeType ?: "application/octet-stream").toMediaTypeOrNull()

    override fun contentLength(): Long = if (contentLength >= 0L) contentLength else -1L

    override fun isOneShot(): Boolean = true

    override fun writeTo(sink: BufferedSink) {
        openInputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var uploaded = 0L

            while (true) {
                if (shouldCancel()) {
                    throw IOException("上传已取消")
                }

                val read = input.read(buffer)
                if (read == -1) break

                sink.write(buffer, 0, read)
                uploaded += read
                onProgress(uploaded)
            }
        }
    }
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(
        object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }
        }
    )
}
