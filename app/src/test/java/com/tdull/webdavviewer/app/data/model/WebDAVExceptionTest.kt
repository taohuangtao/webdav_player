package com.tdull.webdavviewer.app.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * WebDAVException 单元测试
 */
class WebDAVExceptionTest {

    @Test
    fun `ConnectionFailed contains cause message`() {
        val cause = Exception("Network error")
        val exception = WebDAVException.ConnectionFailed(cause)
        assertTrue(exception.message!!.contains("连接失败"))
        assertTrue(exception.message!!.contains("Network error"))
    }

    @Test
    fun `AuthenticationFailed has correct message`() {
        val exception = WebDAVException.AuthenticationFailed()
        assertEquals("认证失败，请检查用户名和密码", exception.message)
    }

    @Test
    fun `ResourceNotFound contains path in message`() {
        val path = "/path/to/file.txt"
        val exception = WebDAVException.ResourceNotFound(path)
        assertTrue(exception.message!!.contains(path))
        assertEquals(path, exception.path)
    }

    @Test
    fun `InvalidResponse contains message`() {
        val exception = WebDAVException.InvalidResponse("Invalid XML")
        assertTrue(exception.message!!.contains("服务器响应无效"))
        assertTrue(exception.message!!.contains("Invalid XML"))
    }

    @Test
    fun `Timeout has correct message`() {
        val exception = WebDAVException.Timeout()
        assertEquals("请求超时，请检查网络连接", exception.message)
    }

    @Test
    fun `ServerError contains status code`() {
        val exception = WebDAVException.ServerError(500, "Internal Server Error")
        assertTrue(exception.message!!.contains("500"))
        assertTrue(exception.message!!.contains("Internal Server Error"))
        assertEquals(500, exception.statusCode)
    }

    @Test
    fun `ServerError with null message uses default`() {
        val exception = WebDAVException.ServerError(503)
        assertTrue(exception.message!!.contains("503"))
        assertTrue(exception.message!!.contains("未知错误"))
        assertEquals(503, exception.statusCode)
    }

    @Test
    fun `all exceptions are instances of WebDAVException`() {
        val exceptions: List<WebDAVException> = listOf(
            WebDAVException.ConnectionFailed(Exception("test")),
            WebDAVException.AuthenticationFailed(),
            WebDAVException.ResourceNotFound("/path"),
            WebDAVException.InvalidResponse("test"),
            WebDAVException.Timeout(),
            WebDAVException.ServerError(500)
        )
        
        exceptions.forEach { exception ->
            assertTrue(exception is WebDAVException)
            assertTrue(exception is Exception)
        }
    }
}
