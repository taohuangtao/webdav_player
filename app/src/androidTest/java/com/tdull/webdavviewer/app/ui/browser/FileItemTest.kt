package com.tdull.webdavviewer.app.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tdull.webdavviewer.app.data.model.DownloadState
import com.tdull.webdavviewer.app.data.model.ResourceType
import com.tdull.webdavviewer.app.data.model.WebDAVResource
import com.tdull.webdavviewer.app.ui.theme.WebDAVViewerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FileItem 更多菜单（Popup）行为测试
 * 回归背景：进入文件浏览器列表页时，菜单曾自动弹出且无法关闭
 * （Popup 渲染条件未检查 showMenu 导致）
 */
@RunWith(AndroidJUnit4::class)
class FileItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testFile = WebDAVResource(
        path = "/video.mp4",
        name = "video.mp4",
        isDirectory = false,
        size = 1024L,
        lastModified = 0L,
        contentType = "video/mp4",
        resourceType = ResourceType.VIDEO
    )

    private val testDirectory = WebDAVResource(
        path = "/folder",
        name = "folder",
        isDirectory = true,
        size = 0L,
        lastModified = 0L,
        contentType = null,
        resourceType = ResourceType.DIRECTORY
    )

    /**
     * 渲染单个 FileItem，并提供一个最小可用的 moreMenuContent：
     * 菜单内容为一个可点击文本，点击后调用 onDismiss 关闭菜单。
     */
    private fun setFileItemContent(resource: WebDAVResource) {
        composeTestRule.setContent {
            WebDAVViewerTheme {
                FileItem(
                    resource = resource,
                    onClick = {},
                    moreMenuContent = { onDismiss ->
                        Text(
                            text = "MENU_ITEM",
                            modifier = Modifier.clickable { onDismiss() }
                        )
                    }
                )
            }
        }
    }

    @Test
    fun moreMenu_isHiddenInitially_fileItem() {
        // 回归：进入页面时菜单不得自动弹出（非目录分支）
        setFileItemContent(testFile)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MENU_ITEM").assertDoesNotExist()
    }

    @Test
    fun moreMenu_isHiddenInitially_directoryItem() {
        // 回归：进入页面时菜单不得自动弹出（目录分支）
        setFileItemContent(testDirectory)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MENU_ITEM").assertDoesNotExist()
    }

    @Test
    fun moreMenu_showsAfterClick_andMenuItemClickCloses() {
        // 点击"更多操作"按钮后菜单显示；点击菜单项（内部调用 onDismiss）后菜单关闭
        setFileItemContent(testFile)
        composeTestRule.onNodeWithContentDescription("更多操作").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MENU_ITEM").assertIsDisplayed()

        composeTestRule.onNodeWithText("MENU_ITEM").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MENU_ITEM").assertDoesNotExist()
    }

    @Test
    fun noInlineFavoriteButton_whenNotFavorite() {
        // 回归：行内不再有收藏按钮（收藏操作收拢到更多菜单）
        setFileItemContent(testFile)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("收藏").assertDoesNotExist()
    }

    @Test
    fun noInlineDownloadButton_whenNotDownloaded() {
        // 回归：行内不再有下载按钮（下载操作收拢到更多菜单）
        setFileItemContent(testFile)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("下载").assertDoesNotExist()
    }

    @Test
    fun downloadProgress_shownWhenDownloading() {
        // 下载中时行内显示进度指示（点击可取消）
        var cancelled = false
        composeTestRule.setContent {
            WebDAVViewerTheme {
                FileItem(
                    resource = testFile,
                    onClick = {},
                    downloadState = DownloadState.Downloading(progressPercent = 45),
                    onCancelDownload = { cancelled = true },
                    moreMenuContent = { onDismiss ->
                        Text(
                            text = "MENU_ITEM",
                            modifier = Modifier.clickable { onDismiss() }
                        )
                    }
                )
            }
        }
        composeTestRule.waitForIdle()
        // 进度百分比文本显示
        composeTestRule.onNodeWithText("45%").assertIsDisplayed()
    }
}
