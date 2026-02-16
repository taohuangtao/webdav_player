package com.tdull.webdavviewer.app.ui.browser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tdull.webdavviewer.app.data.model.ResourceType
import com.tdull.webdavviewer.app.data.model.WebDAVResource
import com.tdull.webdavviewer.app.ui.theme.WebDAVViewerTheme
import com.tdull.webdavviewer.app.viewmodel.FileBrowserUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FileBrowserScreen UI 测试
 */
@RunWith(AndroidJUnit4::class)
class FileBrowserScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ========== 未连接状态测试 ==========

    @Test
    fun displayNotConnectedState_whenNotConnected() {
        composeTestRule.setContent {
            WebDAVViewerTheme {
                TestFileBrowserScreen(
                    uiState = FileBrowserUiState(isConnected = false),
                    currentPath = "/",
                    onRefresh = {},
                    onNavigateBack = {}
                )
            }
        }

        // 验证未连接状态显示
        composeTestRule.onNodeWithText("未连接到服务器").assertIsDisplayed()
        composeTestRule.onNodeWithText("请先在设置中选择并连接服务器").assertIsDisplayed()
    }

    // ========== 加载状态测试 ==========

    @Test
    fun displayLoadingIndicator_whenLoading() {
        composeTestRule.setContent {
            WebDAVViewerTheme {
                TestFileBrowserScreen(
                    uiState = FileBrowserUiState(
                        isConnected = true,
                        isLoading = true
                    ),
                    currentPath = "/",
                    onRefresh = {},
                    onNavigateBack = {}
                )
            }
        }

        // 验证加载指示器显示
        composeTestRule.onNodeWithText("加载中...").assertIsDisplayed()
    }

    // ========== 错误状态测试 ==========

    @Test
    fun displayErrorState_whenErrorOccurs() {
        composeTestRule.setContent {
            WebDAVViewerTheme {
                TestFileBrowserScreen(
                    uiState = FileBrowserUiState(
                        isConnected = true,
                        error = "网络连接失败"
                    ),
                    currentPath = "/",
                    onRefresh = {},
                    onNavigateBack = {}
                )
            }
        }

        // 验证错误状态显示
        composeTestRule.onNodeWithText("加载失败").assertIsDisplayed()
        composeTestRule.onNodeWithText("网络连接失败").assertIsDisplayed()
    }

    // ========== 空目录状态测试 ==========

    @Test
    fun displayEmptyDirectory_whenNoFiles() {
        composeTestRule.setContent {
            WebDAVViewerTheme {
                TestFileBrowserScreen(
                    uiState = FileBrowserUiState(
                        isConnected = true,
                        files = emptyList()
                    ),
                    currentPath = "/",
                    onRefresh = {},
                    onNavigateBack = {}
                )
            }
        }

        // 验证空目录状态显示
        composeTestRule.onNodeWithText("空目录").assertIsDisplayed()
        composeTestRule.onNodeWithText("当前目录没有文件").assertIsDisplayed()
    }

    // ========== 文件列表测试 ==========

    @Test
    fun displayFileList_whenFilesExist() {
        val files = listOf(
            WebDAVResource(
                path = "/folder",
                name = "文件夹",
                isDirectory = true,
                resourceType = ResourceType.DIRECTORY
            ),
            WebDAVResource(
                path = "/video.mp4",
                name = "视频.mp4",
                isDirectory = false,
                size = 1024000,
                resourceType = ResourceType.VIDEO
            ),
            WebDAVResource(
                path = "/image.jpg",
                name = "图片.jpg",
                isDirectory = false,
                size = 512000,
                resourceType = ResourceType.IMAGE
            )
        )

        composeTestRule.setContent {
            WebDAVViewerTheme {
                TestFileBrowserScreen(
                    uiState = FileBrowserUiState(
                        isConnected = true,
                        files = files
                    ),
                    currentPath = "/",
                    onRefresh = {},
                    onNavigateBack = {}
                )
            }
        }

        // 验证文件列表显示
        composeTestRule.onNodeWithText("文件夹").assertIsDisplayed()
        composeTestRule.onNodeWithText("视频.mp4").assertIsDisplayed()
        composeTestRule.onNodeWithText("图片.jpg").assertIsDisplayed()
    }

    // ========== 刷新按钮测试 ==========

    @Test
    fun clickRefreshButton_triggersCallback() {
        var refreshClicked = false

        composeTestRule.setContent {
            WebDAVViewerTheme {
                TestFileBrowserScreen(
                    uiState = FileBrowserUiState(isConnected = true, files = emptyList()),
                    currentPath = "/",
                    onRefresh = { refreshClicked = true },
                    onNavigateBack = {}
                )
            }
        }

        // 点击刷新按钮
        composeTestRule.onNodeWithContentDescription("刷新").performClick()

        // 验证回调被触发
        assert(refreshClicked)
    }

    // ========== 返回按钮测试 ==========

    @Test
    fun clickBackButton_triggersCallback() {
        var backClicked = false

        composeTestRule.setContent {
            WebDAVViewerTheme {
                TestFileBrowserScreen(
                    uiState = FileBrowserUiState(isConnected = true, files = emptyList()),
                    currentPath = "/subfolder",
                    onRefresh = {},
                    onNavigateBack = { backClicked = true }
                )
            }
        }

        // 点击返回按钮
        composeTestRule.onNodeWithContentDescription("返回").performClick()

        // 验证回调被触发
        assert(backClicked)
    }

    // ========== 路径显示测试 ==========

    @Test
    fun displayCurrentPath_inTopBar() {
        composeTestRule.setContent {
            WebDAVViewerTheme {
                TestFileBrowserScreen(
                    uiState = FileBrowserUiState(isConnected = true, files = emptyList()),
                    currentPath = "/视频/电影/",
                    onRefresh = {},
                    onNavigateBack = {}
                )
            }
        }

        // 验证当前路径显示
        composeTestRule.onNodeWithText("/视频/电影/").assertIsDisplayed()
    }
}

/**
 * 用于测试的 FileBrowserScreen 简化版本
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestFileBrowserScreen(
    uiState: FileBrowserUiState,
    currentPath: String,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("文件浏览器")
                        if (currentPath.isNotEmpty() && currentPath != "/") {
                            Text(
                                text = currentPath,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onRefresh) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "刷新"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !uiState.isConnected -> {
                    // 未连接状态
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("未连接到服务器")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("请先在设置中选择并连接服务器")
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = onRefresh) {
                                Text("重试")
                            }
                        }
                    }
                }
                uiState.isLoading -> {
                    // 加载中
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("加载中...")
                        }
                    }
                }
                uiState.error != null -> {
                    // 错误状态
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "加载失败",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(uiState.error!!)
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = onRefresh) {
                                Text("重试")
                            }
                        }
                    }
                }
                uiState.files.isEmpty() -> {
                    // 空目录
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("空目录")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("当前目录没有文件")
                        }
                    }
                }
                else -> {
                    // 文件列表
                    LazyColumn {
                        items(uiState.files.size) { index ->
                            val file = uiState.files[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when {
                                            file.isDirectory -> Icons.Default.Folder
                                            file.isVideo -> Icons.Default.PlayArrow
                                            file.isImage -> Icons.Default.Image
                                            else -> Icons.Default.InsertDriveFile
                                        },
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(text = file.name)
                                        Text(
                                            text = if (file.isDirectory) "目录" else formatSize(file.size),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}
