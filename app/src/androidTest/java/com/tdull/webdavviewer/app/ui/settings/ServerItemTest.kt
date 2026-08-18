package com.tdull.webdavviewer.app.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.ui.theme.WebDAVViewerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ServerItem 更多菜单（更多操作）行为测试
 * 背景：服务器更多菜单从 Material3 DropdownMenu 改为与文件列表一致的自绘公共菜单容器（带动效）
 * 回归：菜单初始不弹出、点击打开、菜单项回调、设为当前仅非活跃显示、关闭
 */
@RunWith(AndroidJUnit4::class)
class ServerItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testServer = ServerConfig(
        id = "1",
        name = "服务器1",
        url = "https://server1.com"
    )

    private fun setServerItemContent(
        isActive: Boolean,
        onActivate: () -> Unit = {},
        onEdit: () -> Unit = {},
        onDelete: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            WebDAVViewerTheme {
                ServerItem(
                    server = testServer,
                    isActive = isActive,
                    onActivate = onActivate,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onClick = {}
                )
            }
        }
    }

    @Test
    fun moreMenu_isHiddenInitially() {
        // 回归：进入页面时菜单不得自动弹出
        setServerItemContent(isActive = false)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("设为当前").assertDoesNotExist()
        composeTestRule.onNodeWithText("编辑").assertDoesNotExist()
        composeTestRule.onNodeWithText("删除").assertDoesNotExist()
    }

    @Test
    fun moreMenu_showsItemsAfterClick() {
        // 点击"更多操作"后菜单项显示
        setServerItemContent(isActive = false)
        composeTestRule.onNodeWithContentDescription("更多操作").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("设为当前").assertIsDisplayed()
        composeTestRule.onNodeWithText("编辑").assertIsDisplayed()
        composeTestRule.onNodeWithText("删除").assertIsDisplayed()
    }

    @Test
    fun activeServer_hidesSetCurrentItem() {
        // 活跃服务器不显示"设为当前"，但显示编辑/删除
        setServerItemContent(isActive = true)
        composeTestRule.onNodeWithContentDescription("更多操作").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("设为当前").assertDoesNotExist()
        composeTestRule.onNodeWithText("编辑").assertIsDisplayed()
        composeTestRule.onNodeWithText("删除").assertIsDisplayed()
    }

    @Test
    fun clickSetCurrent_triggersCallbackAndCloses() {
        // 非活跃服务器点击"设为当前"触发 onActivate 并关闭菜单
        var activated = false
        setServerItemContent(isActive = false, onActivate = { activated = true })
        composeTestRule.onNodeWithContentDescription("更多操作").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("设为当前").performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, activated)
        composeTestRule.onNodeWithText("设为当前").assertDoesNotExist()
    }

    @Test
    fun clickEdit_triggersCallbackAndCloses() {
        // 点击"编辑"触发 onEdit 并关闭菜单
        var edited = false
        setServerItemContent(isActive = false, onEdit = { edited = true })
        composeTestRule.onNodeWithContentDescription("更多操作").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("编辑").performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, edited)
        composeTestRule.onNodeWithText("编辑").assertDoesNotExist()
    }

    @Test
    fun clickDelete_triggersCallbackAndCloses() {
        // 点击"删除"触发 onDelete 并关闭菜单
        var deleted = false
        setServerItemContent(isActive = false, onDelete = { deleted = true })
        composeTestRule.onNodeWithContentDescription("更多操作").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("删除").performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, deleted)
        composeTestRule.onNodeWithText("删除").assertDoesNotExist()
    }
}
