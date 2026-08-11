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

        // 客户端抛非受检异常时，仓库应包装为 ConnectionFailed 失败结果
        `when`(mockClient.connect(config)).thenThrow(RuntimeException("模拟连接异常"))

        val result = repository.connect(config)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WebDAVException.ConnectionFailed)
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
        
        `when`(mockClient.listFiles("/", false)).thenReturn(files)
        
        val result = repository.listFiles("/")
        
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        verify(mockClient).listFiles("/", false)
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
        
        `when`(mockClient.listFiles("/", false)).thenReturn(files)
        
        // 第一次调用
        val result1 = repository.listFiles("/")
        assertTrue(result1.isSuccess)
        
        // 第二次调用应该使用缓存
        val result2 = repository.listFiles("/")
        assertTrue(result2.isSuccess)
        
        // 只调用一次 client.listFiles
        verify(mockClient, times(1)).listFiles("/", false)
    }

    @Test
    fun `listFiles passes showHidden to client`() = runTest {
        val files = listOf(
            WebDAVResource(path = "/.hidden", name = ".hidden", isDirectory = false)
        )
        `when`(mockClient.listFiles("/", true)).thenReturn(files)

        val result = repository.listFiles("/", true)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        verify(mockClient).listFiles("/", true)
    }

    @Test
    fun `cache separates showHidden variants for same path`() = runTest {
        val filteredFiles = listOf(WebDAVResource(path = "/file.txt", name = "file.txt", isDirectory = false))
        val allFiles = listOf(
            WebDAVResource(path = "/file.txt", name = "file.txt", isDirectory = false),
            WebDAVResource(path = "/.hidden", name = ".hidden", isDirectory = false)
        )
        `when`(mockClient.listFiles("/", false)).thenReturn(filteredFiles)
        `when`(mockClient.listFiles("/", true)).thenReturn(allFiles)

        // showHidden=false 加载一次
        repository.listFiles("/", false)
        // showHidden=true 加载一次
        repository.listFiles("/", true)

        // 再以相同参数加载，应命中各自缓存，client 各自只被调用一次
        repository.listFiles("/", false)
        repository.listFiles("/", true)

        verify(mockClient, times(1)).listFiles("/", false)
        verify(mockClient, times(1)).listFiles("/", true)
    }

    @Test
    fun `listFiles returns failure when client throws exception`() = runTest {
        // 客户端抛非受检异常时，仓库应包装为 ConnectionFailed 失败结果
        `when`(mockClient.listFiles("/", false)).thenThrow(RuntimeException("模拟列表异常"))

        val result = repository.listFiles("/")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WebDAVException.ConnectionFailed)
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
        
        `when`(mockClient.listFiles("/", false)).thenReturn(files)
        
        // 加载并缓存
        repository.listFiles("/")
        
        // 清除缓存
        repository.clearCache("/")
        
        // 再次加载应该重新调用 client
        repository.listFiles("/")
        
        verify(mockClient, times(2)).listFiles("/", false)
    }

    @Test
    fun `clearAllCache removes all cached entries`() = runTest {
        val files1 = listOf(WebDAVResource(path = "/file1.txt", name = "file1.txt", isDirectory = false))
        val files2 = listOf(WebDAVResource(path = "/file2.txt", name = "file2.txt", isDirectory = false))
        
        `when`(mockClient.listFiles("/path1", false)).thenReturn(files1)
        `when`(mockClient.listFiles("/path2", false)).thenReturn(files2)
        
        // 加载并缓存两个路径
        repository.listFiles("/path1")
        repository.listFiles("/path2")
        
        // 清除所有缓存
        repository.clearAllCache()
        
        // 再次加载应该重新调用 client
        repository.listFiles("/path1")
        repository.listFiles("/path2")
        
        verify(mockClient, times(2)).listFiles("/path1", false)
        verify(mockClient, times(2)).listFiles("/path2", false)
    }

    // ========== rename / move / delete 测试 ==========

    private fun createResource(path: String, name: String, isDirectory: Boolean = false) =
        WebDAVResource(path = path, name = name, isDirectory = isDirectory)

    @Test
    fun `rename delegates to client and returns success for file`() = runTest {
        val resource = createResource("/folder/aaa.mp4", "aaa.mp4")

        val result = repository.rename(resource, "bbb.mp4")

        assertTrue(result.isSuccess)
        verify(mockClient).rename("/folder/aaa.mp4", "bbb.mp4", false)
    }

    @Test
    fun `rename delegates to client and returns success for directory`() = runTest {
        val resource = createResource("/folder/subdir", "subdir", isDirectory = true)

        val result = repository.rename(resource, "newdir")

        assertTrue(result.isSuccess)
        verify(mockClient).rename("/folder/subdir", "newdir", true)
    }

    @Test
    fun `rename returns failure when client throws exception`() = runTest {
        val resource = createResource("/folder/aaa.mp4", "aaa.mp4")
        `when`(mockClient.rename("/folder/aaa.mp4", "bbb.mp4", false))
            .thenThrow(RuntimeException("模拟异常"))

        val result = repository.rename(resource, "bbb.mp4")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WebDAVException.ConnectionFailed)
    }

    @Test
    fun `rename clears source directory cache`() = runTest {
        val sourceDirFiles = listOf(createResource("/folder/aaa.mp4", "aaa.mp4"))
        `when`(mockClient.listFiles("/folder/", false)).thenReturn(sourceDirFiles)

        // 先加载并缓存源目录
        repository.listFiles("/folder/")
        verify(mockClient, times(1)).listFiles("/folder/", false)

        val resource = createResource("/folder/aaa.mp4", "aaa.mp4")
        repository.rename(resource, "bbb.mp4")

        // 重命名后再次加载源目录应重新调用 client（缓存已清除）
        repository.listFiles("/folder/")
        verify(mockClient, times(2)).listFiles("/folder/", false)
    }

    @Test
    fun `move delegates to client and returns success for file`() = runTest {
        val resource = createResource("/folder/aaa.mp4", "aaa.mp4")

        val result = repository.move(resource, "/videos")

        assertTrue(result.isSuccess)
        verify(mockClient).moveResource("/folder/aaa.mp4", "/videos", false)
    }

    @Test
    fun `move delegates to client and returns success for directory`() = runTest {
        val resource = createResource("/folder/subdir", "subdir", isDirectory = true)

        val result = repository.move(resource, "/videos")

        assertTrue(result.isSuccess)
        verify(mockClient).moveResource("/folder/subdir", "/videos", true)
    }

    @Test
    fun `move returns failure when client throws exception`() = runTest {
        val resource = createResource("/folder/aaa.mp4", "aaa.mp4")
        `when`(mockClient.moveResource("/folder/aaa.mp4", "/videos", false))
            .thenThrow(RuntimeException("模拟异常"))

        val result = repository.move(resource, "/videos")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WebDAVException.ConnectionFailed)
    }

    @Test
    fun `delete delegates to client and returns success for file`() = runTest {
        val resource = createResource("/folder/aaa.mp4", "aaa.mp4")

        val result = repository.delete(resource)

        assertTrue(result.isSuccess)
        verify(mockClient).deleteResource("/folder/aaa.mp4", false)
    }

    @Test
    fun `delete delegates to client and returns success for directory`() = runTest {
        val resource = createResource("/folder/subdir", "subdir", isDirectory = true)

        val result = repository.delete(resource)

        assertTrue(result.isSuccess)
        verify(mockClient).deleteResource("/folder/subdir", true)
    }

    @Test
    fun `delete returns failure when client throws exception`() = runTest {
        val resource = createResource("/folder/aaa.mp4", "aaa.mp4")
        `when`(mockClient.deleteResource("/folder/aaa.mp4", false))
            .thenThrow(RuntimeException("模拟异常"))

        val result = repository.delete(resource)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WebDAVException.ConnectionFailed)
    }
}
