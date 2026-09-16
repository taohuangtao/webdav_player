package com.tdull.webdavviewer.app.viewmodel

import com.tdull.webdavviewer.app.data.model.UploadTask
import com.tdull.webdavviewer.app.data.repository.UploadsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class UploadsViewModelTest {

    @Mock
    private lateinit var uploadsRepository: UploadsRepository

    private lateinit var viewModel: UploadsViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        whenever(uploadsRepository.uploads).thenReturn(flowOf(emptyList<UploadTask>()))
        viewModel = UploadsViewModel(uploadsRepository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `pauseUpload delegates to repository`() = runTest {
        whenever(uploadsRepository.pauseUpload("task-id")).thenReturn(Result.success(Unit))

        viewModel.pauseUpload("task-id")

        verify(uploadsRepository).pauseUpload("task-id")
    }

    @Test
    fun `continueUpload delegates to repository`() = runTest {
        whenever(uploadsRepository.continueUpload("task-id")).thenReturn(Result.success(Unit))

        viewModel.continueUpload("task-id")

        verify(uploadsRepository).continueUpload("task-id")
    }

    @Test
    fun `pauseUpload writes error on failure`() = runTest {
        whenever(uploadsRepository.pauseUpload("task-id"))
            .thenReturn(Result.failure(IllegalStateException("暂停失败")))

        viewModel.pauseUpload("task-id")

        assertEquals("暂停失败", viewModel.uiState.value.error)
    }

    @Test
    fun `continueUpload writes error on failure`() = runTest {
        whenever(uploadsRepository.continueUpload("task-id"))
            .thenReturn(Result.failure(IllegalStateException("继续失败")))

        viewModel.continueUpload("task-id")

        assertEquals("继续失败", viewModel.uiState.value.error)
    }
}
