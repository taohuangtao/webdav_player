package com.tdull.webdavviewer.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.viewmodel.TestConnectionResult
import java.util.UUID

/**
 * 添加/编辑服务器对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerDialog(
    onDismiss: () -> Unit,
    onSave: (ServerConfig) -> Unit,
    onTestConnection: (ServerConfig) -> Unit,
    existingServer: ServerConfig? = null,
    testConnectionResult: TestConnectionResult? = null,
    isLoading: Boolean = false
) {
    // 表单状态
    var name by remember { mutableStateOf(existingServer?.name ?: "") }
    var url by remember { mutableStateOf(existingServer?.url ?: "") }
    var username by remember { mutableStateOf(existingServer?.username ?: "") }
    var password by remember { mutableStateOf(existingServer?.password ?: "") }
    
    // 表单验证状态
    var nameError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }
    
    // 密码可见性
    var passwordVisible by remember { mutableStateOf(false) }

    // 验证表单
    fun validateForm(): Boolean {
        var isValid = true
        
        if (name.isBlank()) {
            nameError = "请输入服务器名称"
            isValid = false
        } else {
            nameError = null
        }
        
        if (url.isBlank()) {
            urlError = "请输入服务器地址"
            isValid = false
        } else if (!isValidUrl(url)) {
            urlError = "请输入有效的URL地址（http://或https://开头）"
            isValid = false
        } else {
            urlError = null
        }
        
        return isValid
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 标题
                Text(
                    text = if (existingServer != null) "编辑服务器" else "添加服务器",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 服务器名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = null
                    },
                    label = { Text("服务器名称") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 服务器地址
                OutlinedTextField(
                    value = url,
                    onValueChange = { 
                        url = it
                        urlError = null
                    },
                    label = { Text("服务器地址") },
                    placeholder = { Text("http://192.168.1.1:8080") },
                    isError = urlError != null,
                    supportingText = urlError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 用户名（可选）
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 密码（可选）
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码（可选）") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(if (passwordVisible) "隐藏" else "显示")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 连接测试结果
                when (testConnectionResult) {
                    is TestConnectionResult.Testing -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在测试连接...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    is TestConnectionResult.Success -> {
                        Text(
                            text = "✓ ${testConnectionResult.message}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is TestConnectionResult.Failed -> {
                        Text(
                            text = "✗ ${testConnectionResult.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    null -> {}
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 测试连接按钮
                    OutlinedButton(
                        onClick = {
                            if (validateForm()) {
                                val config = ServerConfig(
                                    id = existingServer?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    url = url.trim(),
                                    username = username.trim(),
                                    password = password
                                )
                                onTestConnection(config)
                            }
                        },
                        enabled = !isLoading && testConnectionResult !is TestConnectionResult.Testing
                    ) {
                        Text("测试连接")
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // 取消按钮
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    
                    // 保存按钮
                    Button(
                        onClick = {
                            if (validateForm()) {
                                val config = ServerConfig(
                                    id = existingServer?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    url = url.trim(),
                                    username = username.trim(),
                                    password = password
                                )
                                onSave(config)
                            }
                        },
                        enabled = !isLoading
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

/**
 * 验证URL格式
 */
private fun isValidUrl(url: String): Boolean {
    val trimmedUrl = url.trim()
    return (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) 
        && trimmedUrl.length > 7
}
