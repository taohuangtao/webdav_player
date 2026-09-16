package com.tdull.webdavviewer.app.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadTaskTest {

    @Test
    fun `paused upload is not finished`() {
        val task = createTask(status = UploadStatus.PAUSED)

        assertFalse(task.isFinished)
    }

    @Test
    fun `failed canceled completed and conflict uploads are finished`() {
        assertTrue(createTask(status = UploadStatus.FAILED).isFinished)
        assertTrue(createTask(status = UploadStatus.CANCELED).isFinished)
        assertTrue(createTask(status = UploadStatus.COMPLETED).isFinished)
        assertTrue(createTask(status = UploadStatus.WAITING_CONFLICT).isFinished)
    }

    private fun createTask(status: UploadStatus): UploadTask {
        return UploadTask(
            uriString = "content://local/file",
            fileName = "file.png",
            serverId = "server-id",
            serverName = "Server",
            serverUrl = "https://example.com/webdav/",
            targetDirectory = "/",
            targetPath = "/file.png",
            tempPath = "/.file.png.upload.tmp",
            status = status
        )
    }
}
