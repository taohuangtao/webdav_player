package com.tdull.webdavviewer.app.ui.player

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 视频播放器方向切换逻辑单元测试
 * 覆盖 computeTargetOrientation 纯函数：根据当前方向计算点击"切换横竖屏"后的目标方向
 */
class VideoPlayerOrientationTest {

    @Test
    fun `computeTargetOrientation returns portrait when current is landscape`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            computeTargetOrientation(Configuration.ORIENTATION_LANDSCAPE)
        )
    }

    @Test
    fun `computeTargetOrientation returns landscape when current is portrait`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            computeTargetOrientation(Configuration.ORIENTATION_PORTRAIT)
        )
    }

    @Test
    fun `computeTargetOrientation returns landscape when current is undefined`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            computeTargetOrientation(Configuration.ORIENTATION_UNDEFINED)
        )
    }

    @Test
    fun `computeTargetOrientation returns landscape for unknown orientation value`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            computeTargetOrientation(999)
        )
    }
}
