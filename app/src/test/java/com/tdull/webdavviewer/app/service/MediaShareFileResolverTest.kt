package com.tdull.webdavviewer.app.service

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaShareFileResolverTest {

    @Test
    fun `resolveMimeType prefers concrete content type`() {
        val result = MediaShareFileResolver.resolveMimeType(
            title = "photo.jpg",
            url = "https://example.com/photo.jpg",
            explicitMimeType = "image/custom"
        )

        assertEquals("image/custom", result)
    }

    @Test
    fun `resolveMimeType infers image when explicit is wildcard`() {
        val result = MediaShareFileResolver.resolveMimeType(
            title = "photo.jpg",
            url = "https://example.com/photo.jpg",
            explicitMimeType = "image/*"
        )

        assertEquals("image/jpeg", result)
    }

    @Test
    fun `resolveMimeType infers mp4 video`() {
        val result = MediaShareFileResolver.resolveMimeType(
            title = "movie.mp4",
            url = "https://example.com/movie.mp4",
            explicitMimeType = null
        )

        assertEquals("video/mp4", result)
    }

    @Test
    fun `resolveMimeType infers mov video`() {
        val result = MediaShareFileResolver.resolveMimeType(
            title = "clip.mov",
            url = "https://example.com/clip.mov",
            explicitMimeType = null
        )

        assertEquals("video/quicktime", result)
    }

    @Test
    fun `resolveFileName uses decoded url name and sanitizes separators`() {
        val result = MediaShareFileResolver.resolveFileName(
            title = "",
            url = "https://example.com/media/debug%20update%201.png"
        )

        assertEquals("debug update 1.png", result)
        assertEquals(
            "a_b_c.mp4",
            MediaShareFileResolver.resolveFileName(
                title = "a/b\\c.mp4",
                url = "https://example.com/media/a.mp4"
            )
        )
    }
}
