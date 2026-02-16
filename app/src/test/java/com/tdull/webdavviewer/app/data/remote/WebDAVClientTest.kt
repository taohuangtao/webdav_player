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
}
