package com.tdull.webdavviewer.app.data.remote

import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.data.model.WebDAVException
import com.tdull.webdavviewer.app.data.model.WebDAVResource
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * WebDAVClient 单元测试
 * 使用 MockWebServer 模拟 WebDAV 服务器响应
 */
class WebDAVClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: WebDAVClient
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        
        client = WebDAVClient(okHttpClient)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    // ========== testConnection 测试 ==========

    @Test
    fun `testConnection returns true for 200 response`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("<?xml version=\"1.0\"?><D:multistatus xmlns:D=\"DAV:\"></D:multistatus>")
        )

        val config = ServerConfig(
            name = "Test",
            url = mockWebServer.url("/").toString().trimEnd('/')
        )

        val result = client.testConnection(config)
        assertTrue(result)
    }

    @Test
    fun `testConnection returns true for 207 Multi-Status response`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setBody("<?xml version=\"1.0\"?><D:multistatus xmlns:D=\"DAV:\"></D:multistatus>")
        )

        val config = ServerConfig(
            name = "Test",
            url = mockWebServer.url("/").toString().trimEnd('/')
        )

        val result = client.testConnection(config)
        assertTrue(result)
    }

    @Test
    fun `testConnection returns false for 401 response`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("Unauthorized")
        )

        val config = ServerConfig(
            name = "Test",
            url = mockWebServer.url("/").toString().trimEnd('/')
        )

        val result = client.testConnection(config)
        assertFalse(result)
    }

    @Test
    fun `testConnection returns false for connection error`() = runTest {
        val config = ServerConfig(
            name = "Test",
            url = "https://nonexistent-server-12345.invalid"
        )

        val result = client.testConnection(config)
        assertFalse(result)
    }

    // ========== connect 测试 ==========

    @Test
    fun `connect returns true and sets currentConfig`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setBody("<?xml version=\"1.0\"?><D:multistatus xmlns:D=\"DAV:\"></D:multistatus>")
        )

        val config = ServerConfig(
            name = "Test",
            url = mockWebServer.url("/").toString().trimEnd('/')
        )

        val result = client.connect(config)
        assertTrue(result)
    }

    @Test(expected = IllegalStateException::class)
    fun `testConnection without config throws exception`() {
        client.testConnection()
    }

    // ========== listFiles 测试 ==========

    @Test
    fun `listFiles parses multistatus response correctly`() = runTest {
        val xmlResponse = """
            <?xml version="1.0" encoding="utf-8"?>
            <D:multistatus xmlns:D="DAV:">
                <D:response>
                    <D:href>/webdav/</D:href>
                    <D:propstat>
                        <D:prop>
                            <D:displayname>webdav</D:displayname>
                            <D:resourcetype><D:collection/></D:resourcetype>
                        </D:prop>
                        <D:status>HTTP/1.1 200 OK</D:status>
                    </D:propstat>
                </D:response>
                <D:response>
                    <D:href>/webdav/folder/</D:href>
                    <D:propstat>
                        <D:prop>
                            <D:displayname>folder</D:displayname>
                            <D:resourcetype><D:collection/></D:resourcetype>
                        </D:prop>
                        <D:status>HTTP/1.1 200 OK</D:status>
                    </D:propstat>
                </D:response>
                <D:response>
                    <D:href>/webdav/video.mp4</D:href>
                    <D:propstat>
                        <D:prop>
                            <D:displayname>video.mp4</D:displayname>
                            <D:resourcetype/>
                            <D:getcontentlength>1024000</D:getcontentlength>
                            <D:getcontenttype>video/mp4</D:getcontenttype>
                        </D:prop>
                        <D:status>HTTP/1.1 200 OK</D:status>
                    </D:propstat>
                </D:response>
            </D:multistatus>
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setBody(xmlResponse)
        )

        val config = ServerConfig(
            name = "Test",
            url = mockWebServer.url("/webdav/").toString().trimEnd('/')
        )

        client.connect(config)
        val files = client.listFiles("/")

        assertTrue(files.isNotEmpty())
        // 验证目录排序在前
        val directories = files.filter { it.isDirectory }
        val nonDirectories = files.filter { !it.isDirectory }
        assertTrue(directories.all { it.isDirectory })
        assertTrue(nonDirectories.none { it.isDirectory })
    }

    @Test
    fun `listFiles throws AuthenticationFailed for 401`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("Unauthorized")
        )

        val config = ServerConfig(
            name = "Test",
            url = mockWebServer.url("/").toString().trimEnd('/')
        )

        client.connect(config)
        
        val exception = assertThrows(WebDAVException.AuthenticationFailed::class.java) {
            client.listFiles("/")
        }
        assertNotNull(exception)
    }

    @Test
    fun `listFiles throws ResourceNotFound for 404`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("Not Found")
        )

        val config = ServerConfig(
            name = "Test",
            url = mockWebServer.url("/").toString().trimEnd('/')
        )

        client.connect(config)
        
        val exception = assertThrows(WebDAVException.ResourceNotFound::class.java) {
            client.listFiles("/nonexistent")
        }
        assertNotNull(exception)
    }

    @Test
    fun `listFiles throws ConnectionFailed for network error`() = runTest {
        val config = ServerConfig(
            name = "Test",
            url = "https://nonexistent-server-12345.invalid"
        )

        client.connect(config)
        
        val exception = assertThrows(WebDAVException.ConnectionFailed::class.java) {
            client.listFiles("/")
        }
        assertNotNull(exception)
    }

    @Test
    fun `listFiles throws InvalidResponse for empty body`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setBody("")
        )

        val config = ServerConfig(
            name = "Test",
            url = mockWebServer.url("/").toString().trimEnd('/')
        )

        client.connect(config)
        
        val exception = assertThrows(WebDAVException.InvalidResponse::class.java) {
            client.listFiles("/")
        }
        assertNotNull(exception)
    }

    // ========== getStreamUrl 测试 ==========

    @Test
    fun `getStreamUrl returns correct URL`() {
        val config = ServerConfig(
            name = "Test",
            url = "https://example.com/webdav"
        )

        client.connect(config)
        val streamUrl = client.getStreamUrl("/video.mp4")

        assertEquals("https://example.com/webdav/video.mp4", streamUrl)
    }

    @Test(expected = IllegalStateException::class)
    fun `getStreamUrl throws exception without config`() {
        client.getStreamUrl("/video.mp4")
    }

    // ========== 请求验证测试 ==========

    @Test
    fun `PROPFIND request has correct headers`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setBody("<?xml version=\"1.0\"?><D:multistatus xmlns:D=\"DAV:\"></D:multistatus>")
        )

        val config = ServerConfig(
            name = "Test",
            url = mockWebServer.url("/").toString().trimEnd('/')
        )

        client.testConnection(config)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("PROPFIND", recordedRequest.method)
        assertEquals("1", recordedRequest.getHeader("Depth"))
        assertNotNull(recordedRequest.getHeader("Content-Type"))
    }

    @Test
    fun `PROPFIND request includes Basic Auth when credentials provided`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setBody("<?xml version=\"1.0\"?><D:multistatus xmlns:D=\"DAV:\"></D:multistatus>")
        )

        val config = ServerConfig(
            name = "Test",
            url = mockWebServer.url("/").toString().trimEnd('/'),
            username = "testuser",
            password = "testpass"
        )

        client.testConnection(config)

        val recordedRequest = mockWebServer.takeRequest()
        val authHeader = recordedRequest.getHeader("Authorization")
        assertNotNull(authHeader)
        assertTrue(authHeader!!.startsWith("Basic "))
    }

    // ========== rename / moveResource / deleteResource 测试 ==========

    private fun createPropfindConfig(): ServerConfig {
        return ServerConfig(
            name = "Test",
            url = mockWebServer.url("/webdav/").toString().trimEnd('/')
        )
    }

    private fun setupPropfindClient() {
        // 强制使用 PROPFIND 服务器类型，并预置一个 PROPFIND 探测响应
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setBody("<?xml version=\"1.0\"?><D:multistatus xmlns:D=\"DAV:\"></D:multistatus>")
        )
        client.connect(createPropfindConfig())
    }

    @Test
    fun `moveResource sends MOVE request for file without trailing slash`() = runTest {
        setupPropfindClient()
        mockWebServer.enqueue(MockResponse().setResponseCode(201))

        client.moveResource("/folder/aaa.mp4", "/videos")

        // 先消费 connect 探测产生的 PROPFIND 请求
        val probeRequest = mockWebServer.takeRequest()
        assertEquals("PROPFIND", probeRequest.method)
        // 再获取 MOVE 请求
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("MOVE", recordedRequest.method)
        // 文件 URL 不应以 / 结尾
        assertEquals("/webdav/folder/aaa.mp4", recordedRequest.path)
        val destination = recordedRequest.getHeader("Destination")
        assertNotNull(destination)
        // 目标 URL 应指向新目录下的同名文件，且不带尾斜杠
        assertEquals("${mockWebServer.url("/webdav/videos/aaa.mp4")}", destination)
    }

    @Test
    fun `moveResource sends MOVE request for directory with trailing slash`() = runTest {
        setupPropfindClient()
        mockWebServer.enqueue(MockResponse().setResponseCode(201))

        client.moveResource("/folder/subdir", "/videos", isDirectory = true)

        // 先消费 connect 探测产生的 PROPFIND 请求
        val probeRequest = mockWebServer.takeRequest()
        assertEquals("PROPFIND", probeRequest.method)
        // 再获取 MOVE 请求
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("MOVE", recordedRequest.method)
        // 目录 URL 应以 / 结尾
        assertEquals("/webdav/folder/subdir/", recordedRequest.path)
        val destination = recordedRequest.getHeader("Destination")
        assertNotNull(destination)
        assertEquals("${mockWebServer.url("/webdav/videos/subdir/")}", destination)
    }

    @Test
    fun `rename sends MOVE request with renamed Destination for file`() = runTest {
        setupPropfindClient()
        mockWebServer.enqueue(MockResponse().setResponseCode(201))

        client.rename("/folder/aaa.mp4", "bbb.mp4")

        // 先消费 connect 探测产生的 PROPFIND 请求
        val probeRequest = mockWebServer.takeRequest()
        assertEquals("PROPFIND", probeRequest.method)
        // 再获取 MOVE 请求
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("MOVE", recordedRequest.method)
        // 文件 URL 不应以 / 结尾
        assertEquals("/webdav/folder/aaa.mp4", recordedRequest.path)
        val destination = recordedRequest.getHeader("Destination")
        assertNotNull(destination)
        // 重命名后目标 URL 应为同一目录下的新名称，且不带尾斜杠
        assertEquals("${mockWebServer.url("/webdav/folder/bbb.mp4")}", destination)
    }

    @Test
    fun `rename sends MOVE request with renamed Destination for directory`() = runTest {
        setupPropfindClient()
        mockWebServer.enqueue(MockResponse().setResponseCode(201))

        client.rename("/folder/subdir", "newdir", isDirectory = true)

        // 先消费 connect 探测产生的 PROPFIND 请求
        val probeRequest = mockWebServer.takeRequest()
        assertEquals("PROPFIND", probeRequest.method)
        // 再获取 MOVE 请求
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("MOVE", recordedRequest.method)
        assertEquals("/webdav/folder/subdir/", recordedRequest.path)
        val destination = recordedRequest.getHeader("Destination")
        assertNotNull(destination)
        assertEquals("${mockWebServer.url("/webdav/folder/newdir/")}", destination)
    }

    @Test
    fun `deleteResource sends DELETE request for file without trailing slash`() = runTest {
        setupPropfindClient()
        mockWebServer.enqueue(MockResponse().setResponseCode(204))

        client.deleteResource("/folder/aaa.mp4")

        // 先消费 connect 探测产生的 PROPFIND 请求
        val probeRequest = mockWebServer.takeRequest()
        assertEquals("PROPFIND", probeRequest.method)
        // 再获取 DELETE 请求
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("DELETE", recordedRequest.method)
        assertEquals("/webdav/folder/aaa.mp4", recordedRequest.path)
    }

    @Test
    fun `deleteResource sends DELETE request for directory with trailing slash`() = runTest {
        setupPropfindClient()
        mockWebServer.enqueue(MockResponse().setResponseCode(204))

        client.deleteResource("/folder/subdir", isDirectory = true)

        // 先消费 connect 探测产生的 PROPFIND 请求
        val probeRequest = mockWebServer.takeRequest()
        assertEquals("PROPFIND", probeRequest.method)
        // 再获取 DELETE 请求
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("DELETE", recordedRequest.method)
        assertEquals("/webdav/folder/subdir/", recordedRequest.path)
    }

    @Test
    fun `deleteResource throws OperationFailed for 409 conflict`() = runTest {
        setupPropfindClient()
        mockWebServer.enqueue(MockResponse().setResponseCode(409))

        val exception = assertThrows(WebDAVException.OperationFailed::class.java) {
            client.deleteResource("/folder/aaa.mp4")
        }
        assertNotNull(exception)
    }

    @Test
    fun `deleteResource throws AuthenticationFailed for 401`() = runTest {
        setupPropfindClient()
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        val exception = assertThrows(WebDAVException.AuthenticationFailed::class.java) {
            client.deleteResource("/folder/aaa.mp4")
        }
        assertNotNull(exception)
    }

    @Test
    fun `moveResource throws UnsupportedOperation for autoindex server`() = runTest {
        val config = ServerConfig(
            name = "Test",
            url = mockWebServer.url("/").toString().trimEnd('/')
        )
        // 先用 PROPFIND 建立连接和配置
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setBody("<?xml version=\"1.0\"?><D:multistatus xmlns:D=\"DAV:\"></D:multistatus>")
        )
        client.connect(config)
        // 再强制标记为 autoindex 服务器
        client.setServerType(ServerType.AUTOINDEX)

        val exception = assertThrows(WebDAVException.UnsupportedOperation::class.java) {
            client.moveResource("/folder/aaa.mp4", "/videos")
        }
        assertNotNull(exception)
    }
}
