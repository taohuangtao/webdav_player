package com.tdull.webdavviewer.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.tdull.webdavviewer.app.R
import com.tdull.webdavviewer.app.data.model.WebDAVException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 错误类型枚举
 */
enum class ErrorType {
    NETWORK_UNAVAILABLE,      // 无网络连接
    CONNECTION_TIMEOUT,       // 连接超时
    SERVER_UNREACHABLE,       // 服务器无响应
    AUTHENTICATION_FAILED,    // 认证失败
    RESOURCE_NOT_FOUND,       // 资源不存在
    UNSUPPORTED_FORMAT,       // 不支持的格式
    SERVER_ERROR,             // 服务器错误
    INVALID_RESPONSE,         // 无效响应
    UNKNOWN                   // 未知错误
}

/**
 * 错误信息数据类
 */
data class ErrorInfo(
    val type: ErrorType,
    val title: String,
    val message: String,
    val canRetry: Boolean = true,
    val actionText: String? = null
)

/**
 * 错误处理工具类
 * 将各种异常转换为用户友好的错误信息
 */
object ErrorHandler {
    
    /**
     * 将异常转换为错误信息
     */
    fun getErrorInfo(throwable: Throwable, context: Context): ErrorInfo {
        return when (throwable) {
            // WebDAV特定异常
            is WebDAVException.ConnectionFailed -> {
                when (throwable.cause) {
                    is SocketTimeoutException -> ErrorInfo(
                        type = ErrorType.CONNECTION_TIMEOUT,
                        title = context.getString(R.string.error_connection_timeout_title),
                        message = context.getString(R.string.error_connection_timeout_message),
                        canRetry = true
                    )
                    is UnknownHostException -> ErrorInfo(
                        type = ErrorType.SERVER_UNREACHABLE,
                        title = context.getString(R.string.error_server_unreachable_title),
                        message = context.getString(R.string.error_server_unreachable_message),
                        canRetry = true
                    )
                    else -> ErrorInfo(
                        type = ErrorType.SERVER_UNREACHABLE,
                        title = context.getString(R.string.error_connection_failed_title),
                        message = throwable.message,
                        canRetry = true
                    )
                }
            }
            is WebDAVException.AuthenticationFailed -> ErrorInfo(
                type = ErrorType.AUTHENTICATION_FAILED,
                title = context.getString(R.string.error_auth_failed_title),
                message = context.getString(R.string.error_auth_failed_message),
                canRetry = false,
                actionText = context.getString(R.string.action_edit_credentials)
            )
            is WebDAVException.ResourceNotFound -> ErrorInfo(
                type = ErrorType.RESOURCE_NOT_FOUND,
                title = context.getString(R.string.error_not_found_title),
                message = context.getString(R.string.error_not_found_message, throwable.path),
                canRetry = false
            )
            is WebDAVException.InvalidResponse -> ErrorInfo(
                type = ErrorType.INVALID_RESPONSE,
                title = context.getString(R.string.error_invalid_response_title),
                message = throwable.message,
                canRetry = true
            )
            is WebDAVException.Timeout -> ErrorInfo(
                type = ErrorType.CONNECTION_TIMEOUT,
                title = context.getString(R.string.error_connection_timeout_title),
                message = context.getString(R.string.error_connection_timeout_message),
                canRetry = true
            )
            is WebDAVException.ServerError -> ErrorInfo(
                type = ErrorType.SERVER_ERROR,
                title = context.getString(R.string.error_server_error_title),
                message = context.getString(R.string.error_server_error_message, throwable.statusCode),
                canRetry = true
            )
            
            // 网络相关异常
            is SocketTimeoutException -> ErrorInfo(
                type = ErrorType.CONNECTION_TIMEOUT,
                title = context.getString(R.string.error_connection_timeout_title),
                message = context.getString(R.string.error_connection_timeout_message),
                canRetry = true
            )
            is UnknownHostException -> {
                if (!isNetworkAvailable(context)) {
                    ErrorInfo(
                        type = ErrorType.NETWORK_UNAVAILABLE,
                        title = context.getString(R.string.error_no_network_title),
                        message = context.getString(R.string.error_no_network_message),
                        canRetry = true
                    )
                } else {
                    ErrorInfo(
                        type = ErrorType.SERVER_UNREACHABLE,
                        title = context.getString(R.string.error_server_unreachable_title),
                        message = context.getString(R.string.error_server_unreachable_message),
                        canRetry = true
                    )
                }
            }
            
            // 其他异常
            else -> ErrorInfo(
                type = ErrorType.UNKNOWN,
                title = context.getString(R.string.error_unknown_title),
                message = throwable.message ?: context.getString(R.string.error_unknown_message),
                canRetry = true
            )
        }
    }
    
    /**
     * 检查网络是否可用
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * 获取不支持的格式错误信息
     */
    fun getUnsupportedFormatError(context: Context, format: String): ErrorInfo {
        return ErrorInfo(
            type = ErrorType.UNSUPPORTED_FORMAT,
            title = context.getString(R.string.error_unsupported_format_title),
            message = context.getString(R.string.error_unsupported_format_message, format),
            canRetry = false
        )
    }
    
    /**
     * 获取简单错误消息（用于Toast或简单提示）
     */
    fun getSimpleErrorMessage(throwable: Throwable, context: Context): String {
        return getErrorInfo(throwable, context).message
    }
}
