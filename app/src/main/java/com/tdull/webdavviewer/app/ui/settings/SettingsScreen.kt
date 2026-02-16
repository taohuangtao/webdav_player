package com.tdull.webdavviewer.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.viewmodel.SettingsViewModel

/**
 * 设置页面 - 服务器配置管理
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToBrowser: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加服务器")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 服务器列表标题
            Text(
                text = "服务器列表",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            // 服务器列表
            if (uiState.servers.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无服务器配置",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击右下角按钮添加服务器",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(
                        items = uiState.servers,
                        key = { it.id }
                    ) { server ->
                        ServerItem(
                            server = server,
                            isActive = server.id == uiState.activeServerId,
                            onActivate = { viewModel.setActiveServer(server.id) },
                            onEdit = { viewModel.showEditDialog(server) },
                            onDelete = { viewModel.showDeleteConfirm(server) },
                            onClick = { 
                                viewModel.setActiveServer(server.id)
                                onNavigateToBrowser(server.id)
                            }
                        )
                    }
                }
            }
        }

        // 添加服务器对话框
        if (uiState.showAddDialog) {
            AddServerDialog(
                onDismiss = { viewModel.hideAddDialog() },
                onSave = { config ->
                    viewModel.saveServer(config)
                },
                onTestConnection = { config ->
                    viewModel.testConnection(config)
                },
                testConnectionResult = uiState.testConnectionResult,
                isLoading = uiState.isLoading
            )
        }

        // 编辑服务器对话框
        if (uiState.showEditDialog && uiState.editingServer != null) {
            AddServerDialog(
                onDismiss = { viewModel.hideEditDialog() },
                onSave = { config ->
                    viewModel.saveServer(config)
                },
                onTestConnection = { config ->
                    viewModel.testConnection(config)
                },
                existingServer = uiState.editingServer,
                testConnectionResult = uiState.testConnectionResult,
                isLoading = uiState.isLoading
            )
        }

        // 删除确认对话框
        if (uiState.showDeleteConfirm && uiState.serverToDelete != null) {
            val serverToDelete = uiState.serverToDelete!!
            AlertDialog(
                onDismissRequest = { viewModel.hideDeleteConfirm() },
                title = { Text("确认删除") },
                text = { Text("确定要删除服务器 \"${serverToDelete.name}\" 吗？") },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.deleteServer() }
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideDeleteConfirm() }) {
                        Text("取消")
                    }
                }
            )
        }

        // 错误提示
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // 这里可以显示 Snackbar，暂时用 AlertDialog
            }
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("错误") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("确定")
                    }
                }
            )
        }
    }
}

/**
 * 服务器列表项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerItem(
    server: ServerConfig,
    isActive: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 1.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 服务器图标
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 服务器信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = server.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (server.requiresAuth()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "需要认证",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // 激活状态标签
            if (isActive) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "当前",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 更多操作菜单
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多操作")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (!isActive) {
                        DropdownMenuItem(
                            text = { Text("设为当前") },
                            onClick = {
                                onActivate()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = {
                            onEdit()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            ) 
                        }
                    )
                }
            }
        }
    }
}
