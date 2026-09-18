package com.tdull.webdavviewer.app.service

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.tdull.webdavviewer.app.data.model.MediaSharePayload
import com.tdull.webdavviewer.app.data.model.MediaShareRequest
import com.tdull.webdavviewer.app.data.remote.WebDAVClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class MediaShareManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("WebDAV") private val okHttpClient: OkHttpClient,
    private val webDAVClient: WebDAVClient
) {
    companion object {
        private const val BUFFER_SIZE = 8192
        private const val SHARE_CACHE_DIR = "share_media"
        private const val CACHE_MAX_AGE_MS = 24L * 60L * 60L * 1000L
    }

    suspend fun prepareShare(
        request: MediaShareRequest,
        onProgress: (Int?) -> Unit = {}
    ): Result<MediaSharePayload> = withContext(Dispatchers.IO) {
        try {
            val title = MediaShareFileResolver.resolveFileName(
                title = request.title,
                url = request.url
            )
            val mimeType = MediaShareFileResolver.resolveMimeType(
                title = title,
                url = request.url,
                explicitMimeType = request.mimeType
            )

            if (request.url.startsWith("file:", ignoreCase = true)) {
                val localFile = File(Uri.parse(request.url).path ?: "")
                if (!localFile.exists()) {
                    return@withContext Result.failure(Exception("文件不存在，无法分享"))
                }
                return@withContext Result.success(
                    MediaSharePayload(
                        uri = getShareUri(localFile),
                        title = title,
                        mimeType = mimeType
                    )
                )
            }

            prepareRemoteShare(
                url = request.url,
                title = title,
                mimeType = mimeType,
                onProgress = onProgress
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun prepareRemoteShare(
        url: String,
        title: String,
        mimeType: String,
        onProgress: (Int?) -> Unit
    ): Result<MediaSharePayload> {
        cleanupOldCache()
        val localFile = createShareCacheFile(title)
        return try {
            onProgress(0)

            val call = okHttpClient.newCall(buildRequest(url))
            val response = executeCancellable(call)
            response.use {
                if (!it.isSuccessful) {
                    localFile.delete()
                    return Result.failure(Exception("准备分享失败: HTTP ${it.code}"))
                }

                val body = it.body
                if (body == null) {
                    localFile.delete()
                    return Result.failure(Exception("响应体为空"))
                }

                val totalBytes = body.contentLength()
                val buffer = ByteArray(BUFFER_SIZE)
                var copiedBytes = 0L

                FileOutputStream(localFile).use { output ->
                    body.byteStream().use { input ->
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val bytesRead = input.read(buffer)
                            if (bytesRead == -1) {
                                break
                            }
                            output.write(buffer, 0, bytesRead)
                            copiedBytes += bytesRead
                            if (totalBytes > 0) {
                                onProgress(((copiedBytes * 100) / totalBytes).toInt().coerceIn(0, 100))
                            } else {
                                onProgress(null)
                            }
                        }
                    }
                }
            }

            Result.success(
                MediaSharePayload(
                    uri = getShareUri(localFile),
                    title = title,
                    mimeType = mimeType
                )
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            localFile.delete()
            throw e
        } catch (e: Exception) {
            localFile.delete()
            Result.failure(e)
        }
    }

    private fun buildRequest(url: String): Request {
        val requestBuilder = Request.Builder().url(url)
        val config = webDAVClient.getCurrentConfig()
        if (config != null && config.requiresAuth()) {
            requestBuilder.header("Authorization", Credentials.basic(config.username, config.password))
        }
        return requestBuilder.build()
    }

    private suspend fun executeCancellable(call: Call): Response {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                call.cancel()
            }
            try {
                val response = call.execute()
                if (continuation.isActive) {
                    continuation.resume(response)
                } else {
                    response.close()
                }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    private fun createShareCacheFile(title: String): File {
        val dir = File(context.cacheDir, SHARE_CACHE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        var file = File(dir, title)
        if (!file.exists()) {
            return file
        }

        val dotIndex = title.lastIndexOf('.')
        val baseName = if (dotIndex > 0) title.substring(0, dotIndex) else title
        val extension = if (dotIndex > 0) title.substring(dotIndex) else ""
        var index = 1
        while (file.exists()) {
            file = File(dir, "$baseName($index)$extension")
            index++
        }
        return file
    }

    private fun getShareUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun cleanupOldCache() {
        val dir = File(context.cacheDir, SHARE_CACHE_DIR)
        if (!dir.exists()) {
            return
        }
        val cutoff = System.currentTimeMillis() - CACHE_MAX_AGE_MS
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }
}

internal object MediaShareFileResolver {
    fun resolveFileName(title: String, url: String): String {
        val rawName = title.takeIf { it.isNotBlank() }
            ?: extractNameFromUrl(url)
            ?: "shared_media"
        return rawName
            .replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""), "_")
            .trim()
            .takeIf { it.isNotBlank() }
            ?: "shared_media"
    }

    fun resolveMimeType(
        title: String,
        url: String,
        explicitMimeType: String?
    ): String {
        val normalizedExplicit = explicitMimeType
            ?.substringBefore(";")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        if (normalizedExplicit != null && !normalizedExplicit.endsWith("/*")) {
            return normalizedExplicit
        }

        return inferMimeType(title)
            ?: inferMimeType(extractNameFromUrl(url).orEmpty())
            ?: normalizedExplicit
            ?: "application/octet-stream"
    }

    private fun extractNameFromUrl(url: String): String? {
        return runCatching {
            val path = java.net.URI(url).path.orEmpty()
            URLDecoder.decode(path.substringAfterLast('/'), "UTF-8")
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun inferMimeType(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.US)
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            "heic" -> "image/heic"
            "heif" -> "image/heif"
            "mp4", "m4v" -> "video/mp4"
            "mov" -> "video/quicktime"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "mpeg", "mpg" -> "video/mpeg"
            "3gp" -> "video/3gpp"
            else -> null
        }
    }
}
