package com.tdull.webdavviewer.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.viewmodel.SettingsViewModel

// ================= 设置页设计稿配色（settings_redesign.html） =================
private val SettingsBg = Color(0xFFF4F6FB)      // 页面背景
private val CardWhite = Color(0xFFFFFFFF)        // 卡片底色
private val TextPrimary = Color(0xFF111827)      // 主文字
private val TextSecondary = Color(0xFF6B7280)    // 次级文字
private val TextMuted = Color(0xFF9CA3AF)        // 弱化文字
private val IndigoFab = Color(0xFF6366F1)        // FAB 主色
private val IndigoPrimary = Color(0xFF4F46E5)    // 图标/标签主色
private val IndigoLight = Color(0xFFEEF2FF)      // indigo 浅底
private val RosePrimary = Color(0xFFF43F5E)      // 收藏红
private val RoseLight = Color(0xFFFFF1F2)        // 收藏浅底

/**
 * 设置页面 - 服务器配置管理
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToBrowser: (String) -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = SettingsBg,
        topBar = {
            // 紧凑顶部导航栏（降低高度，与首页风格一致）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SettingsBg)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "设置",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = IndigoFab,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
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
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp)
            )

            // 我的收藏入口
            EntryCard(
                icon = Icons.Default.Favorite,
                iconTint = RosePrimary,
                iconBg = RoseLight,
                title = "我的收藏",
                onClick = onNavigateToFavorites
            )

            // 已下载入口
            EntryCard(
                icon = Icons.Default.Download,
                iconTint = IndigoPrimary,
                iconBg = IndigoLight,
                title = "已下载",
                onClick = onNavigateToDownloads
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                            tint = TextMuted
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无服务器配置",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击右下角按钮添加服务器",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
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
 * 入口卡片（我的收藏 / 已下载）——设计稿风格：白底圆角卡片 + 浅彩底图标方块 + 右箭头
 */
@Composable
private fun EntryCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 浅彩底图标方块
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * 服务器列表项 ——设计稿风格：白底圆角卡片 + indigo 图标方块 + 名称/URL/认证标签 + "当前"胶囊 + 更多菜单
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
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 服务器图标（indigo 浅底方块）
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(IndigoLight, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 服务器信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = server.url,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (server.requiresAuth()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "需要认证",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // 激活状态标签（胶囊）
            if (isActive) {
                Box(
                    modifier = Modifier
                        .background(IndigoLight, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "当前",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IndigoPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 更多操作菜单
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "更多操作",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
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
