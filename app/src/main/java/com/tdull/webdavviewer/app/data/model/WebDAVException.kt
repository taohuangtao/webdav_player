package com.tdull.webdavviewer.app.data.model

/**
 * WebDAV操作相关的异常类
 */
sealed class WebDAVException : Exception() {
    
    /**
     * 连接失败异常
     */
    class ConnectionFailed(cause: Throwable) : WebDAVException() {
        override val message: String = "连接失败: ${cause.message}"
    }
    
    /**
     * 认证失败异常
     */
    class AuthenticationFailed : WebDAVException() {
        override val message: String = "认证失败，请检查用户名和密码"
    }
    
    /**
     * 资源未找到异常
     */
    class ResourceNotFound(val path: String) : WebDAVException() {
        override val message: String = "资源不存在: $path"
    }
    
    /**
     * 无效响应异常
     */
    class InvalidResponse(message: String) : WebDAVException() {
        override val message: String = "服务器响应无效: $message"
    }
    
    /**
     * 网络超时异常
     */
    class Timeout : WebDAVException() {
        override val message: String = "请求超时，请检查网络连接"
    }
    
    /**
     * 服务器错误异常
     */
    class ServerError(val statusCode: Int, message: String? = null) : WebDAVException() {
        override val message: String = "服务器错误 ($statusCode): ${message ?: "未知错误"}"
    }

    /**
     * 写操作失败异常（重命名/移动/删除）
     * 例如：目标已存在（409/400）、操作被拒绝（403）等
     */
    class OperationFailed(message: String) : WebDAVException() {
        override val message: String = message
    }

    /**
     * 服务器不支持写操作异常
     * 例如：Nginx autoindex 服务器不支持 MOVE/DELETE
     */
    class UnsupportedOperation : WebDAVException() {
        override val message: String = "当前服务器不支持此操作"
    }
}
