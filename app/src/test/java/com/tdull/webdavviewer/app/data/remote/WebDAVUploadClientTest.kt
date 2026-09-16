package com.tdull.webdavviewer.app.data.remote

import com.tdull.webdavviewer.app.data.model.UploadConflictPolicy
import com.tdull.webdavviewer.app.data.model.UploadTask
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class WebDAVUploadClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: WebDAVUploadClient

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        client = WebDAVUploadClient(
            OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
        )
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `uploadFile puts final file when skipping conflicts`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201))
        val task = createTask(conflictPolicy = UploadConflictPolicy.SKIP)
        var latestProgress = 0L

        client.uploadFile(
            task = task,
            openInputStream = { "hello".byteInputStream() },
            onProgress = { latestProgress = it },
            shouldCancel = { false }
        )

        val putRequest = mockWebServer.takeRequest()
        assertEquals("PUT", putRequest.method)
        assertEquals("/webdav/photos/vacation%20photo%231.png", putRequest.path)
        assertEquals("hello", putRequest.body.readUtf8())
        assertEquals(5L, latestProgress)
    }

    @Test
    fun `uploadFile puts final file when policy is overwrite`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(201))
        val task = createTask(conflictPolicy = UploadConflictPolicy.OVERWRITE)

        client.uploadFile(
            task = task,
            openInputStream = { "hello".byteInputStream() },
            onProgress = {},
            shouldCancel = { false }
        )

        val putRequest = mockWebServer.takeRequest()
        assertEquals("PUT", putRequest.method)
        assertEquals("/webdav/photos/vacation%20photo%231.png", putRequest.path)
    }

    @Test
    fun `uploadFile throws conflict exception for 412 put response`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(412))
        val task = createTask(conflictPolicy = UploadConflictPolicy.SKIP)

        try {
            client.uploadFile(
                task = task,
                openInputStream = { "hello".byteInputStream() },
                onProgress = {},
                shouldCancel = { false }
            )
            fail("Expected UploadConflictException")
        } catch (e: UploadConflictException) {
            // Expected.
        }
    }

    private fun createTask(conflictPolicy: UploadConflictPolicy): UploadTask {
        return UploadTask(
            id = "task-id",
            uriString = "content://local/file",
            fileName = "vacation photo#1.png",
            mimeType = "image/png",
            fileSize = 5L,
            serverId = "server-id",
            serverName = "Server",
            serverUrl = mockWebServer.url("/webdav/").toString().trimEnd('/'),
            targetDirectory = "/photos/",
            targetPath = "/photos/vacation photo#1.png",
            tempPath = "/photos/.vacation photo#1.png.upload-task-id.tmp",
            conflictPolicy = conflictPolicy,
            totalBytes = 5L
        )
    }
}
