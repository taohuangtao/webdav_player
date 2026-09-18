package com.tdull.webdavviewer.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tdull.webdavviewer.app.data.model.MediaSharePayload
import com.tdull.webdavviewer.app.viewmodel.MediaShareViewModel

@Composable
fun MediaShareHost(
    viewModel: MediaShareViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.payload) {
        val payload = uiState.payload ?: return@LaunchedEffect
        try {
            context.startActivity(createShareChooser(payload))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "没有可用的分享应用", Toast.LENGTH_SHORT).show()
        }
        viewModel.clearPayload()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    if (uiState.isPreparing) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("准备分享") },
            text = {
                Column {
                    Text(
                        text = uiState.progressPercent?.let { "正在准备文件 $it%" }
                            ?: "正在准备文件..."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val progressPercent = uiState.progressPercent
                    if (progressPercent != null) {
                        LinearProgressIndicator(
                            progress = { progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.cancelShare() }) {
                    Text("取消")
                }
            }
        )
    }
}

private fun createShareChooser(payload: MediaSharePayload): Intent {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = payload.mimeType
        putExtra(Intent.EXTRA_STREAM, payload.uri)
        putExtra(Intent.EXTRA_TITLE, payload.title)
        putExtra(Intent.EXTRA_SUBJECT, payload.title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(sendIntent, "分享 ${payload.title}")
}
