package com.tdull.webdavviewer.app.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * WebDAVResource 单元测试
 */
class WebDAVResourceTest {

    // ========== determineResourceType 测试 ==========

    @Test
    fun `determineResourceType returns DIRECTORY for directory`() {
        val result = WebDAVResource.determineResourceType(
            name = "folder",
            contentType = null,
            isDirectory = true
        )
        assertEquals(ResourceType.DIRECTORY, result)
    }

    @Test
    fun `determineResourceType returns VIDEO for mp4 extension`() {
        val result = WebDAVResource.determineResourceType(
            name = "video.mp4",
            contentType = null,
            isDirectory = false
        )
        assertEquals(ResourceType.VIDEO, result)
    }

    @Test
    fun `determineResourceType returns VIDEO for mkv extension`() {
        val result = WebDAVResource.determineResourceType(
            name = "movie.mkv",
            contentType = null,
            isDirectory = false
        )
        assertEquals(ResourceType.VIDEO, result)
    }

    @Test
    fun `determineResourceType returns VIDEO for video mime type`() {
        val result = WebDAVResource.determineResourceType(
            name = "file",
            contentType = "video/mp4",
            isDirectory = false
        )
        assertEquals(ResourceType.VIDEO, result)
    }

    @Test
    fun `determineResourceType returns IMAGE for jpg extension`() {
        val result = WebDAVResource.determineResourceType(
            name = "photo.jpg",
            contentType = null,
            isDirectory = false
        )
        assertEquals(ResourceType.IMAGE, result)
    }

    @Test
    fun `determineResourceType returns IMAGE for png extension`() {
        val result = WebDAVResource.determineResourceType(
            name = "image.PNG",
            contentType = null,
            isDirectory = false
        )
        assertEquals(ResourceType.IMAGE, result)
    }

    @Test
    fun `determineResourceType returns IMAGE for image mime type`() {
        val result = WebDAVResource.determineResourceType(
            name = "file",
            contentType = "image/jpeg",
            isDirectory = false
        )
        assertEquals(ResourceType.IMAGE, result)
    }

    @Test
    fun `determineResourceType returns AUDIO for mp3 extension`() {
        val result = WebDAVResource.determineResourceType(
            name = "song.mp3",
            contentType = null,
            isDirectory = false
        )
        assertEquals(ResourceType.AUDIO, result)
    }

    @Test
    fun `determineResourceType returns AUDIO for audio mime type`() {
        val result = WebDAVResource.determineResourceType(
            name = "file",
            contentType = "audio/mpeg",
            isDirectory = false
        )
        assertEquals(ResourceType.AUDIO, result)
    }

    @Test
    fun `determineResourceType returns OTHER for unknown file`() {
        val result = WebDAVResource.determineResourceType(
            name = "document.pdf",
            contentType = null,
            isDirectory = false
        )
        assertEquals(ResourceType.OTHER, result)
    }

    @Test
    fun `determineResourceType returns OTHER for unknown mime type`() {
        val result = WebDAVResource.determineResourceType(
            name = "file",
            contentType = "application/pdf",
            isDirectory = false
        )
        assertEquals(ResourceType.OTHER, result)
    }

    @Test
    fun `determineResourceType is case insensitive`() {
        val result = WebDAVResource.determineResourceType(
            name = "VIDEO.MP4",
            contentType = null,
            isDirectory = false
        )
        assertEquals(ResourceType.VIDEO, result)
    }

    // ========== WebDAVResource 属性测试 ==========

    @Test
    fun `isVideo returns true for VIDEO resource type`() {
        val resource = WebDAVResource(
            path = "/video.mp4",
            name = "video.mp4",
            isDirectory = false,
            resourceType = ResourceType.VIDEO
        )
        assertTrue(resource.isVideo)
        assertFalse(resource.isImage)
    }

    @Test
    fun `isImage returns true for IMAGE resource type`() {
        val resource = WebDAVResource(
            path = "/image.jpg",
            name = "image.jpg",
            isDirectory = false,
            resourceType = ResourceType.IMAGE
        )
        assertTrue(resource.isImage)
        assertFalse(resource.isVideo)
    }

    @Test
    fun `default values are correct`() {
        val resource = WebDAVResource(
            path = "/file.txt",
            name = "file.txt",
            isDirectory = false
        )
        assertEquals(0L, resource.size)
        assertEquals(0L, resource.lastModified)
        assertNull(resource.contentType)
        assertEquals(ResourceType.OTHER, resource.resourceType)
    }
}
