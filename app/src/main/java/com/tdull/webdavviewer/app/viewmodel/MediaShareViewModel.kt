package com.tdull.webdavviewer.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdull.webdavviewer.app.data.model.MediaShareRequest
import com.tdull.webdavviewer.app.data.model.MediaShareUiState
import com.tdull.webdavviewer.app.service.MediaShareManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaShareViewModel @Inject constructor(
    private val mediaShareManager: MediaShareManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaShareUiState())
    val uiState: StateFlow<MediaShareUiState> = _uiState.asStateFlow()

    private var prepareJob: Job? = null

    fun prepareShare(request: MediaShareRequest) {
        prepareJob?.cancel()
        prepareJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPreparing = true,
                    progressPercent = 0,
                    error = null,
                    payload = null
                )
            }

            val result = mediaShareManager.prepareShare(request) { progress ->
                _uiState.update { it.copy(progressPercent = progress) }
            }

            result.fold(
                onSuccess = { payload ->
                    _uiState.update {
                        it.copy(
                            isPreparing = false,
                            progressPercent = null,
                            error = null,
                            payload = payload
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isPreparing = false,
                            progressPercent = null,
                            error = error.message ?: "准备分享失败",
                            payload = null
                        )
                    }
                }
            )
        }
    }

    fun cancelShare() {
        prepareJob?.cancel()
        prepareJob = null
        _uiState.update {
            it.copy(
                isPreparing = false,
                progressPercent = null,
                error = null,
                payload = null
            )
        }
    }

    fun clearPayload() {
        _uiState.update { it.copy(payload = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        prepareJob?.cancel()
        super.onCleared()
    }
}
