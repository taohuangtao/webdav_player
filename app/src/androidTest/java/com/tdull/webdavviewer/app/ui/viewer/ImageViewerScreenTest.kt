package com.tdull.webdavviewer.app.ui.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.TouchInjectionScope
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
        setImageViewer(initialIndex = 1)

        composeTestRule.onNodeWithText("two  2 / 3").assertIsDisplayed()
    }

    @Test
    fun imageViewer_swipeLeftShowsNextImageTitle() {
        setImageViewer(initialIndex = 0)

        composeTestRule.onRoot().performTouchInput {
            swipeLeft()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("two  2 / 3").assertIsDisplayed()
    }

    @Test
    fun imageViewer_pinchZoomDoesNotSwitchPage() {
        setImageViewer(initialIndex = 0)

        composeTestRule.onRoot().performTouchInput {
            pinchOut()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("one  1 / 3").assertIsDisplayed()
    }

    @Test
    fun imageViewer_dragAfterZoomDoesNotSwitchPage() {
        setImageViewer(initialIndex = 0)

        composeTestRule.onRoot().performTouchInput {
            doubleClick(center)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput {
            down(center)
            repeat(8) {
                moveBy(Offset(-24f, 0f))
            }
            up()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("one  1 / 3").assertIsDisplayed()
    }

    @Test
    fun imageViewer_swipeWorksAgainAfterResetZoom() {
        setImageViewer(initialIndex = 0)

        composeTestRule.onRoot().performTouchInput {
            doubleClick(center)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("重置").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput {
            swipeLeft()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("two  2 / 3").assertIsDisplayed()
    }

    private fun setImageViewer(initialIndex: Int = 0) {
        composeTestRule.setContent {
            WebDAVViewerTheme {
                ImageViewerScreen(
                    imageUrl = "https://example.com/one.jpg",
                    items = testItems(),
                    initialIndex = initialIndex,
                    onBack = {}
                )
            }
        }
    }

    private fun TouchInjectionScope.pinchOut() {
        val firstStart = center + Offset(-24f, 0f)
        val secondStart = center + Offset(24f, 0f)
        down(0, firstStart)
        down(1, secondStart)
        repeat(8) {
            updatePointerBy(0, Offset(-12f, 0f))
            updatePointerBy(1, Offset(12f, 0f))
            move()
        }
        up(1)
        up(0)
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
