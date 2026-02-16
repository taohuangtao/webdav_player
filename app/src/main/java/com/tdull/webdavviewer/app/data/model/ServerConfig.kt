package com.tdull.webdavviewer.app.data.model

import java.util.UUID

data class ServerConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val username: String = "",
    val password: String = ""
) {
    init {
        require(url.isNotBlank()) { "URL不能为空" }
    }
    
    /**
     * 获取格式化的URL（确保以/结尾）
     */
    fun getNormalizedUrl(): String {
        return if (url.endsWith("/")) url else "$url/"
    }
    
    /**
     * 获取基础URL（移除结尾的/）
     */
    fun getBaseUrl(): String {
        return url.trimEnd('/')
    }
    
    /**
     * 检查是否需要认证
     */
    fun requiresAuth(): Boolean = username.isNotBlank() || password.isNotBlank()
}
