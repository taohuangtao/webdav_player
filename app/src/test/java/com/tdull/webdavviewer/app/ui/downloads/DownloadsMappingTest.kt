package com.tdull.webdavviewer.app.ui.downloads

import com.tdull.webdavviewer.app.data.model.DownloadItem
import com.tdull.webdavviewer.app.data.model.ResourceType
import com.tdull.webdavviewer.app.ui.downloads.toWebDAVResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadsMappingTest {

    @Test
    fun `download maps to WebDAVResource video with size and time`() {
        val download = DownloadItem(
            videoUrl = "https://example.com/video.mp4",
            videoTitle = "下载视频.mp4",
            serverId = "server1",
            resourcePath = "/folder/video.mp4",
            localPath = "/storage/video.mp4",
            fileSize = 1024L,
            downloadedAt = 987654321L
        )

        val resource = download.toWebDAVResource()

        assertEquals("/folder/video.mp4", resource.path)
        assertEquals("下载视频.mp4", resource.name)
        assertFalse(resource.isDirectory)
        assertEquals(ResourceType.VIDEO, resource.resourceType)
        assertTrue(resource.isVideo)
        assertEquals(1024L, resource.size)
        assertEquals(987654321L, resource.lastModified)
    }

    @Test
    fun `download with empty title falls back to 未命名视频`() {
        val download = DownloadItem(
            videoUrl = "https://example.com/a.mp4",
            videoTitle = "",
            serverId = "server1",
            resourcePath = "/a.mp4",
            localPath = "/a.mp4",
            fileSize = 0L,
            downloadedAt = 1L
        )

        val resource = download.toWebDAVResource()

        assertEquals("未命名视频", resource.name)
    }
}
