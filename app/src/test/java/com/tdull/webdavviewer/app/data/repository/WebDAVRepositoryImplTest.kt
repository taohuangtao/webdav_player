package com.tdull.webdavviewer.app.data.repository

import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.data.model.WebDAVException
import com.tdull.webdavviewer.app.data.model.WebDAVResource
import com.tdull.webdavviewer.app.data.remote.WebDAVClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

/**
 * WebDAVRepositoryImpl 单元测试
 */
class WebDAVRepositoryImplTest {

    private lateinit var mockClient: WebDAVClient
    private lateinit var repository: WebDAVRepositoryImpl

    @Before
    fun setup() {
        mockClient = mock()
        repository = WebDAVRepositoryImpl(mockClient)
    }

    // ========== connect 测试 ==========

    @Test
    fun `connect returns success when client connects successfully`() = runTest {
        val config = ServerConfig(
            name = "Test",
            url = "https://example.com"
        )
        
        `when`(mockClient.connect(config)).thenReturn(true)
        
        val result = repository.connect(config)
        
        assertTrue(result.isSuccess)
        verify(mockClient).connect(config)
    }

    @Test
    fun `connect returns failure when client returns false`() = runTest {
        val config = ServerConfig(
            name = "Test",
            url = "https://example.com"
        )
        
        `when`(mockClient.connect(config)).thenReturn(false)
        
        val result = repository.connect(config)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WebDAVException.ConnectionFailed)
    }

    @Test
    fun `connect returns failure when client throws exception`() = runTest {
        val config = ServerConfig(
            name = "Test",
            url = "https://example.com"
        )
        
        `when`(mockClient.connect(config)).thenThrow(WebDAVException.AuthenticationFailed())
        
        val result = repository.connect(config)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WebDAVException.AuthenticationFailed)
    }

    // ========== listFiles 测试 ==========

    @Test
    fun `listFiles returns success with file list`() = runTest {
        val files = listOf(
            WebDAVResource(
                path = "/folder",
                name = "folder",
                isDirectory = true
            ),
            WebDAVResource(
                path = "/file.txt",
                name = "file.txt",
                isDirectory = false,
                size = 100
            )
        )
        
        `when`(mockClient.listFiles("/")).thenReturn(files)
        
        val result = repository.listFiles("/")
        
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        verify(mockClient).listFiles("/")
    }

    @Test
    fun `listFiles returns cached result on second call`() = runTest {
        val files = listOf(
            WebDAVResource(
                path = "/file.txt",
                name = "file.txt",
                isDirectory = false
            )
        )
        
        `when`(mockClient.listFiles("/")).thenReturn(files)
        
        // 第一次调用
        val result1 = repository.listFiles("/")
        assertTrue(result1.isSuccess)
        
        // 第二次调用应该使用缓存
        val result2 = repository.listFiles("/")
        assertTrue(result2.isSuccess)
        
        // 只调用一次 client.listFiles
        verify(mockClient, times(1)).listFiles("/")
    }

    @Test
    fun `listFiles returns failure when client throws exception`() = runTest {
        `when`(mockClient.listFiles("/")).thenThrow(WebDAVException.ResourceNotFound("/missing"))
        
        val result = repository.listFiles("/")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WebDAVException.ResourceNotFound)
    }

    // ========== testConnection 测试 ==========

    @Test
    fun `testConnection returns success when connection succeeds`() = runTest {
        val config = ServerConfig(
            name = "Test",
            url = "https://example.com"
        )
        
        `when`(mockClient.testConnection(config)).thenReturn(true)
        
        val result = repository.testConnection(config)
        
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun `testConnection returns failure on exception`() = runTest {
        val config = ServerConfig(
            name = "Test",
            url = "https://example.com"
        )
        
        `when`(mockClient.testConnection(config)).thenThrow(RuntimeException("Network error"))
        
        val result = repository.testConnection(config)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WebDAVException.ConnectionFailed)
    }

    // ========== getStreamUrl 测试 ==========

    @Test
    fun `getStreamUrl delegates to client`() {
        val expectedUrl = "https://example.com/video.mp4"
        `when`(mockClient.getStreamUrl("/video.mp4")).thenReturn(expectedUrl)
        
        val result = repository.getStreamUrl("/video.mp4")
        
        assertEquals(expectedUrl, result)
        verify(mockClient).getStreamUrl("/video.mp4")
    }

    // ========== 缓存测试 ==========

    @Test
    fun `getCacheStats returns correct initial stats`() {
        val stats = repository.getCacheStats()
        
        assertEquals(0, stats.size)
        assertEquals(50, stats.maxSize)
        assertTrue(stats.paths.isEmpty())
    }

    @Test
    fun `clearCache removes cached entry`() = runTest {
        val files = listOf(
            WebDAVResource(
                path = "/file.txt",
                name = "file.txt",
                isDirectory = false
            )
        )
        
        `when`(mockClient.listFiles("/")).thenReturn(files)
        
        // 加载并缓存
        repository.listFiles("/")
        
        // 清除缓存
        repository.clearCache("/")
        
        // 再次加载应该重新调用 client
        repository.listFiles("/")
        
        verify(mockClient, times(2)).listFiles("/")
    }

    @Test
    fun `clearAllCache removes all cached entries`() = runTest {
        val files1 = listOf(WebDAVResource(path = "/file1.txt", name = "file1.txt", isDirectory = false))
        val files2 = listOf(WebDAVResource(path = "/file2.txt", name = "file2.txt", isDirectory = false))
        
        `when`(mockClient.listFiles("/path1")).thenReturn(files1)
        `when`(mockClient.listFiles("/path2")).thenReturn(files2)
        
        // 加载并缓存两个路径
        repository.listFiles("/path1")
        repository.listFiles("/path2")
        
        // 清除所有缓存
        repository.clearAllCache()
        
        // 再次加载应该重新调用 client
        repository.listFiles("/path1")
        repository.listFiles("/path2")
        
        verify(mockClient, times(2)).listFiles("/path1")
        verify(mockClient, times(2)).listFiles("/path2")
    }
}
