package com.tdull.webdavviewer.app.viewmodel

import android.net.Uri
import com.tdull.webdavviewer.app.data.model.MediaSharePayload
import com.tdull.webdavviewer.app.data.model.MediaShareRequest
import com.tdull.webdavviewer.app.service.MediaShareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MediaShareViewModelTest {

    @Mock
    private lateinit var mediaShareManager: MediaShareManager

    private lateinit var viewModel: MediaShareViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = MediaShareViewModel(mediaShareManager)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `prepareShare writes payload on success`() = runTest {
        val request = MediaShareRequest(
            url = "https://example.com/photo.jpg",
            title = "photo.jpg",
            mimeType = "image/jpeg"
        )
        val payload = MediaSharePayload(
            uri = mock<Uri>(),
            title = "photo.jpg",
            mimeType = "image/jpeg"
        )
        whenever(mediaShareManager.prepareShare(any(), any())).thenReturn(Result.success(payload))

        viewModel.prepareShare(request)

        val state = viewModel.uiState.value
        assertFalse(state.isPreparing)
        assertEquals(payload, state.payload)
        assertEquals(null, state.error)
    }

    @Test
    fun `prepareShare writes error on failure`() = runTest {
        whenever(mediaShareManager.prepareShare(any(), any()))
            .thenReturn(Result.failure(IllegalStateException("准备失败")))

        viewModel.prepareShare(
            MediaShareRequest(
                url = "https://example.com/video.mp4",
                title = "video.mp4",
                mimeType = "video/mp4"
            )
        )

        val state = viewModel.uiState.value
        assertFalse(state.isPreparing)
        assertEquals("准备失败", state.error)
        assertEquals(null, state.payload)
    }
}
