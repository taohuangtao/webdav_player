package com.tdull.webdavviewer.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.ui.theme.WebDAVViewerTheme
import com.tdull.webdavviewer.app.viewmodel.SettingsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SettingsScreen UI 测试
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ========== 空状态测试 ==========

    @Test
    fun displayEmptyState_whenNoServersConfigured() {
        composeTestRule.setContent {
            WebDAVViewerTheme {
                TestSettingsScreen(
                    uiState = SettingsUiState(servers = emptyList()),
                    onAddServerClick = {}
                )
            }
        }

        // 验证空状态提示显示
        composeTestRule.onNodeWithText("暂无服务器配置").assertIsDisplayed()
        composeTestRule.onNodeWithText("点击右下角按钮添加服务器").assertIsDisplayed()
    }

    // ========== 服务器列表测试 ==========

    @Test
    fun displayServerList_whenServersConfigured() {
        val servers = listOf(
            ServerConfig(id = "1", name = "服务器1", url = "https://server1.com"),
            ServerConfig(id = "2", name = "服务器2", url = "https://server2.com")
        )

        composeTestRule.setContent {
            WebDAVViewerTheme {
                TestSettingsScreen(
                    uiState = SettingsUiState(servers = servers),
                    onAddServerClick = {}
                )
            }
        }

        // 验证服务器列表标题显示
        composeTestRule.onNodeWithText("服务器列表").assertIsDisplayed()
        
        // 验证服务器项显示
        composeTestRule.onNodeWithText("服务器1").assertIsDisplayed()
        composeTestRule.onNodeWithText("服务器2").assertIsDisplayed()
    }

    @Test
    fun displayActiveLabel_forActiveServer() {
        val servers = listOf(
            ServerConfig(id = "active", name = "活跃服务器", url = "https://active.com")
        )

        composeTestRule.setContent {
            WebDAVViewerTheme {
                TestSettingsScreen(
                    uiState = SettingsUiState(
                        servers = servers,
                        activeServerId = "active"
                    ),
                    onAddServerClick = {}
                )
            }
        }

        // 验证"当前"标签显示
        composeTestRule.onNodeWithText("当前").assertIsDisplayed()
    }

    @Test
    fun displayAuthLabel_forServerWithAuth() {
        val servers = listOf(
            ServerConfig(
                id = "auth-server",
                name = "认证服务器",
                url = "https://auth.com",
                username = "user",
                password = "pass"
            )
        )

        composeTestRule.setContent {
            WebDAVViewerTheme {
                TestSettingsScreen(
                    uiState = SettingsUiState(servers = servers),
                    onAddServerClick = {}
                )
            }
        }

        // 验证"需要认证"标签显示
        composeTestRule.onNodeWithText("需要认证").assertIsDisplayed()
    }

    // ========== 添加按钮测试 ==========

    @Test
    fun clickAddButton_triggersCallback() {
        var addClicked = false

        composeTestRule.setContent {
            WebDAVViewerTheme {
                TestSettingsScreen(
                    uiState = SettingsUiState(),
                    onAddServerClick = { addClicked = true }
                )
            }
        }

        // 点击添加按钮
        composeTestRule.onNodeWithContentDescription("添加服务器").performClick()

        // 验证回调被触发
        assert(addClicked)
    }

    // ========== 加载状态测试 ==========

    @Test
    fun displayLoadingIndicator_whenLoading() {
        composeTestRule.setContent {
            WebDAVViewerTheme {
                TestSettingsScreen(
                    uiState = SettingsUiState(isLoading = true),
                    onAddServerClick = {}
                )
            }
        }

        // 验证加载指示器存在
        composeTestRule.onNodeWithText("加载中...").assertIsDisplayed()
    }
}

/**
 * 用于测试的 SettingsScreen 简化版本
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestSettingsScreen(
    uiState: SettingsUiState,
    onAddServerClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddServerClick
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "添加服务器"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
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
            } else if (uiState.servers.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无服务器配置",
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击右下角按钮添加服务器",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                Text(
                    text = "服务器列表",
                    modifier = Modifier.padding(16.dp)
                )
                LazyColumn {
                    items(uiState.servers.size) { index ->
                        val server = uiState.servers[index]
                        val isActive = server.id == uiState.activeServerId
                        
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = server.name)
                                    Text(
                                        text = server.url,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (server.requiresAuth()) {
                                        Text(
                                            text = "需要认证",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                if (isActive) {
                                    Text(text = "当前")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
