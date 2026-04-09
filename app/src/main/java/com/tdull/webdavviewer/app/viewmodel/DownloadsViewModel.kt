package com.tdull.webdavviewer.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdull.webdavviewer.app.data.model.DownloadItem
import com.tdull.webdavviewer.app.data.repository.DownloadsRepository
import com.tdull.webdavviewer.app.service.DownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * 下载列表UI状态
 */
data class DownloadsUiState(
    val isLoading: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val itemToDelete: DownloadItem? = null,
    val error: String? = null
)

/**
 * 下载列表ViewModel
 */
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadsRepository: DownloadsRepository,
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    // 下载列表
    val downloads: StateFlow<List<DownloadItem>> = downloadsRepository.downloads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * 显示删除确认对话框
     */
    fun showDeleteConfirm(item: DownloadItem) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = true,
            itemToDelete = item
        )
    }

    /**
     * 隐藏删除确认对话框
     */
    fun hideDeleteConfirm() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirm = false,
            itemToDelete = null
        )
    }

    /**
     * 删除下载项
     */
    fun deleteDownload() {
        val item = _uiState.value.itemToDelete ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            downloadManager.deleteDownload(item.id)
                .onSuccess {
                    hideDeleteConfirm()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "删除失败"
                    )
                }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 获取本地文件的播放URL
     */
    fun getLocalVideoUrl(localPath: String): String {
        val file = File(localPath)
        return if (file.exists()) {
            file.toURI().toString()
        } else {
            ""
        }
    }

    /**
     * 检查本地文件是否存在
     */
    fun isFileExists(localPath: String): Boolean {
        return File(localPath).exists()
    }

    /**
     * 获取下载目录信息
     */
    fun getDownloadInfo(): DownloadDirectoryInfo {
        val dir = File(downloadManager.getDownloadDirectory())
        val totalSpace = downloadManager.getTotalSpace()
        val availableSpace = downloadManager.getAvailableSpace()
        val usedByDownloads = downloads.value.sumOf { it.fileSize }

        return DownloadDirectoryInfo(
            path = dir.absolutePath,
            totalSpace = totalSpace,
            availableSpace = availableSpace,
            usedByDownloads = usedByDownloads
        )
    }
}

/**
 * 下载目录信息
 */
data class DownloadDirectoryInfo(
    val path: String,
    val totalSpace: Long,
    val availableSpace: Long,
    val usedByDownloads: Long
)
