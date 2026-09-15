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

    private fun String.encode(): String = URLEncoder.encode(this, "UTF-8")
}
