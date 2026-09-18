package com.tdull.webdavviewer.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URLEncoder

class ScreenTest {

    @Test
    fun `ImageViewer createRoute encodes url thumbnailUrl and title`() {
        val url = "https://example.com/webdav/photos/photo #1.png"
        val title = "photo #1.png"
        val thumbnailUrl = "https://example.com/webdav/photos/.thumbs/photo #1.png.m.jpg"

        val route = Screen.ImageViewer.createRoute(
            url = url,
            title = title,
            thumbnailUrl = thumbnailUrl
        )

        assertEquals(
            "image?url=${url.encode()}&title=${title.encode()}&thumbnailUrl=${thumbnailUrl.encode()}",
            route
        )
    }

    @Test
    fun `VideoPlayer createRoute encodes url and title`() {
        val url = "https://example.com/webdav/videos/movie #1.mp4"
        val title = "movie #1.mp4"

        val route = Screen.VideoPlayer.createRoute(url = url, title = title)

        assertEquals(
            "video?url=${url.encode()}&title=${title.encode()}",
            route
        )
    }

    private fun String.encode(): String = URLEncoder.encode(this, "UTF-8")
}
