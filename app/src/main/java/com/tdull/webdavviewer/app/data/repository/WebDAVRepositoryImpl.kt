package com.tdull.webdavviewer.app.data.repository

import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.data.model.WebDAVException
import com.tdull.webdavviewer.app.data.model.WebDAVResource
import com.tdull.webdavviewer.app.data.remote.WebDAVClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 缓存条目
 */
private data class CacheEntry(
    val files: List<WebDAVResource>,
    val timestamp: Long
)

/**
 * WebDAV数据仓库实现
 */
@Singleton
class WebDAVRepositoryImpl @Inject constructor(
    private val client: WebDAVClient
) : WebDAVRepository {
    
    // 内存缓存 - 使用线程安全的实现
    private val cache = mutableMapOf<String, CacheEntry>()
    private val cacheMutex = Mutex()
    
    // 缓存配置
    private val cacheTimeout = TimeUnit.MINUTES.toMillis(2) // 2分钟缓存
    private val maxCacheSize = 50 // 最大缓存条目数
    
    override suspend fun connect(config: ServerConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val success = client.connect(config)
            if (success) {
                // 连接成功时清除旧缓存
                clearAllCache()
                Result.success(Unit)
            } else {
                Result.failure(WebDAVException.ConnectionFailed(Exception("连接失败")))
            }
        } catch (e: WebDAVException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(WebDAVException.ConnectionFailed(e))
        }
    }
    
    override suspend fun listFiles(path: String): Result<List<WebDAVResource>> = withContext(Dispatchers.IO) {
        // 检查缓存
        val cachedResult = getCachedResult(path)
        if (cachedResult != null) {
            return@withContext Result.success(cachedResult)
        }
        
        try {
            val files = client.listFiles(path)
            // 更新缓存
            setCachedResult(path, files)
            Result.success(files)
        } catch (e: WebDAVException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(WebDAVException.ConnectionFailed(e))
        }
    }
    
    override fun getStreamUrl(path: String): String {
        return client.getStreamUrl(path)
    }
    
    override suspend fun testConnection(config: ServerConfig): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val success = client.testConnection(config)
            Result.success(success)
        } catch (e: WebDAVException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(WebDAVException.ConnectionFailed(e))
        }
    }
    
    /**
     * 获取缓存的目录列表（线程安全）
     */
    private suspend fun getCachedResult(path: String): List<WebDAVResource>? {
        return cacheMutex.withLock {
            val entry = cache[path] ?: return null
            
            // 检查缓存是否过期
            if (System.currentTimeMillis() - entry.timestamp > cacheTimeout) {
                cache.remove(path)
                return null
            }
            
            entry.files
        }
    }
    
    /**
     * 设置缓存（线程安全）
     */
    private suspend fun setCachedResult(path: String, files: List<WebDAVResource>) {
        cacheMutex.withLock {
            // 如果缓存已满，移除最旧的条目
            if (cache.size >= maxCacheSize && !cache.containsKey(path)) {
                val oldestKey = cache.minByOrNull { it.value.timestamp }?.key
                oldestKey?.let { cache.remove(it) }
            }
            
            cache[path] = CacheEntry(files, System.currentTimeMillis())
        }
    }
    
    /**
     * 清除指定路径的缓存
     */
    suspend fun clearCache(path: String) {
        cacheMutex.withLock {
            cache.remove(path)
        }
    }
    
    /**
     * 清除所有缓存
     */
    suspend fun clearAllCache() {
        cacheMutex.withLock {
            cache.clear()
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            size = cache.size,
            maxSize = maxCacheSize,
            paths = cache.keys.toList()
        )
    }
}

/**
 * 缓存统计信息
 */
data class CacheStats(
    val size: Int,
    val maxSize: Int,
    val paths: List<String>
)
