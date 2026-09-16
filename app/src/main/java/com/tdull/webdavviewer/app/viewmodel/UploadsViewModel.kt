package com.tdull.webdavviewer.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdull.webdavviewer.app.data.model.UploadTask
import com.tdull.webdavviewer.app.data.repository.UploadsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadsUiState(
    val error: String? = null
)

@HiltViewModel
class UploadsViewModel @Inject constructor(
    private val uploadsRepository: UploadsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadsUiState())
    val uiState: StateFlow<UploadsUiState> = _uiState.asStateFlow()

    val uploads: StateFlow<List<UploadTask>> = uploadsRepository.uploads
        .map { tasks -> tasks.sortedByDescending { it.createdAt } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun cancelUpload(id: String) {
        viewModelScope.launch {
            uploadsRepository.cancelUpload(id)
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "取消上传失败") }
                }
        }
    }

    fun pauseUpload(id: String) {
        viewModelScope.launch {
            uploadsRepository.pauseUpload(id)
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "暂停上传失败") }
                }
        }
    }

    fun continueUpload(id: String) {
        viewModelScope.launch {
            uploadsRepository.continueUpload(id)
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "继续上传失败") }
                }
        }
    }

    fun retryUpload(id: String) {
        viewModelScope.launch {
            uploadsRepository.retryUpload(id)
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "重试上传失败") }
                }
        }
    }

    fun clearFinishedUploads() {
        viewModelScope.launch {
            uploadsRepository.clearFinishedUploads()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
