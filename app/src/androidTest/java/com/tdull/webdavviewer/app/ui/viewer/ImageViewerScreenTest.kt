package com.tdull.webdavviewer.app.ui.viewer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tdull.webdavviewer.app.ui.theme.WebDAVViewerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageViewerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun imageViewer_showsInitialPageIndicator() {
        composeTestRule.setContent {
            WebDAVViewerTheme {
                ImageViewerScreen(
                    imageUrl = "https://example.com/one.jpg",
                    items = testItems(),
                    initialIndex = 1,
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("two  2 / 3").assertIsDisplayed()
    }

    @Test
    fun imageViewer_swipeLeftShowsNextImageTitle() {
        composeTestRule.setContent {
            WebDAVViewerTheme {
                ImageViewerScreen(
                    imageUrl = "https://example.com/one.jpg",
                    items = testItems(),
                    initialIndex = 0,
                    onBack = {}
                )
            }
        }

        composeTestRule.onRoot().performTouchInput {
            swipeLeft()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("two  2 / 3").assertIsDisplayed()
    }

    private fun testItems(): List<ImageViewerItem> = listOf(
        ImageViewerItem(
            url = "https://example.com/one.jpg",
            mediumThumbnailUrl = "https://example.com/.thumbs/one.jpg.m.jpg",
            title = "one"
        ),
        ImageViewerItem(
            url = "https://example.com/two.jpg",
            mediumThumbnailUrl = "https://example.com/.thumbs/two.jpg.m.jpg",
            title = "two"
        ),
        ImageViewerItem(
            url = "https://example.com/three.jpg",
            mediumThumbnailUrl = "https://example.com/.thumbs/three.jpg.m.jpg",
            title = "three"
        )
    )
}
