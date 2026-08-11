package com.tdull.webdavviewer.app.ui.favorites

import com.tdull.webdavviewer.app.data.model.FavoriteItem
import com.tdull.webdavviewer.app.data.model.ResourceType
import com.tdull.webdavviewer.app.ui.favorites.toWebDAVResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesMappingTest {

    @Test
    fun `favorite maps to WebDAVResource video`() {
        val favorite = FavoriteItem(
            videoUrl = "https://example.com/video.mp4",
            videoTitle = "测试视频.mp4",
            serverId = "server1",
            resourcePath = "/folder/video.mp4",
            addedAt = 123456789L
        )

        val resource = favorite.toWebDAVResource()

        assertEquals("/folder/video.mp4", resource.path)
        assertEquals("测试视频.mp4", resource.name)
        assertFalse(resource.isDirectory)
        assertEquals(ResourceType.VIDEO, resource.resourceType)
        assertTrue(resource.isVideo)
        assertEquals(123456789L, resource.lastModified)
    }

    @Test
    fun `favorite with empty title falls back to 未命名视频`() {
        val favorite = FavoriteItem(
            videoUrl = "https://example.com/a.mp4",
            videoTitle = "",
            serverId = "server1",
            resourcePath = "/a.mp4",
            addedAt = 1L
        )

        val resource = favorite.toWebDAVResource()

        assertEquals("未命名视频", resource.name)
    }

    @Test
    fun `favorite default size and non-directory`() {
        val favorite = FavoriteItem(
            videoUrl = "https://example.com/b.mp4",
            videoTitle = "b.mp4",
            serverId = "server1",
            resourcePath = "/b.mp4"
        )

        val resource = favorite.toWebDAVResource()

        assertEquals(0L, resource.size)
        assertFalse(resource.isDirectory)
    }
}
