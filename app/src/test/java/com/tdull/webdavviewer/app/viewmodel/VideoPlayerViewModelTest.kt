package com.tdull.webdavviewer.app.viewmodel

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * VideoPlayerViewModel 单元测试
 * 聚焦拖动 seek 的边界与崩溃回归（元数据未加载时 duration 为 TIME_UNSET）
 */
class VideoPlayerViewModelTest {

    // ========== computeDragSeekTarget 崩溃回归 ==========

    @Test
    fun `computeDragSeekTarget returns null when duration is TIME_UNSET`() {
        // 元数据未加载，duration 为 C.TIME_UNSET（Long.MIN_VALUE + 1）
        val result = computeDragSeekTarget(
            currentPosition = 0L,
            dragSeekOffset = 10_000L,
            duration = C.TIME_UNSET
        )
        // 返回 null，调用方应跳过 seek，避免 coerceIn 空区间崩溃
        assertNull(result)
    }

    @Test
    fun `computeDragSeekTarget returns null when duration is zero`() {
        val result = computeDragSeekTarget(
            currentPosition = 0L,
            dragSeekOffset = 5_000L,
            duration = 0L
        )
        assertNull(result)
    }

    @Test
    fun `computeDragSeekTarget returns null when duration is negative`() {
        val result = computeDragSeekTarget(
            currentPosition = 100L,
            dragSeekOffset = 1_000L,
            duration = -1L
        )
        assertNull(result)
    }

    // ========== computeDragSeekTarget 正常 seek 逻辑 ==========

    @Test
    fun `computeDragSeekTarget returns currentPosition plus offset within duration`() {
        val result = computeDragSeekTarget(
            currentPosition = 30_000L,
            dragSeekOffset = 5_000L,
            duration = 100_000L
        )
        assertEquals(35_000L, result)
    }

    @Test
    fun `computeDragSeekTarget clamps negative target to zero`() {
        val result = computeDragSeekTarget(
            currentPosition = 2_000L,
            dragSeekOffset = -10_000L,
            duration = 100_000L
        )
        assertEquals(0L, result)
    }

    @Test
    fun `computeDragSeekTarget clamps target beyond duration`() {
        val result = computeDragSeekTarget(
            currentPosition = 90_000L,
            dragSeekOffset = 20_000L,
            duration = 100_000L
        )
        assertEquals(100_000L, result)
    }

    @Test
    fun `computeDragSeekTarget allows seeking to exact end of duration`() {
        val result = computeDragSeekTarget(
            currentPosition = 90_000L,
            dragSeekOffset = 10_000L,
            duration = 100_000L
        )
        assertEquals(100_000L, result)
    }
}
