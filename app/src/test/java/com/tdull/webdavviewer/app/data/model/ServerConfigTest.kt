package com.tdull.webdavviewer.app.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * ServerConfig 单元测试
 */
class ServerConfigTest {

    // ========== 构造函数验证测试 ==========

    @Test
    fun `ServerConfig creates with valid URL`() {
        val config = ServerConfig(
            name = "Test Server",
            url = "https://example.com/webdav"
        )
        assertEquals("Test Server", config.name)
        assertEquals("https://example.com/webdav", config.url)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ServerConfig throws exception for blank URL`() {
        ServerConfig(
            name = "Test Server",
            url = ""
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ServerConfig throws exception for whitespace URL`() {
        ServerConfig(
            name = "Test Server",
            url = "   "
        )
    }

    @Test
    fun `ServerConfig generates UUID for id by default`() {
        val config = ServerConfig(
            name = "Test",
            url = "https://example.com"
        )
        assertTrue(config.id.isNotEmpty())
    }

    // ========== getNormalizedUrl 测试 ==========

    @Test
    fun `getNormalizedUrl adds trailing slash when missing`() {
        val config = ServerConfig(
            name = "Test",
            url = "https://example.com/webdav"
        )
        assertEquals("https://example.com/webdav/", config.getNormalizedUrl())
    }

    @Test
    fun `getNormalizedUrl keeps trailing slash when present`() {
        val config = ServerConfig(
            name = "Test",
            url = "https://example.com/webdav/"
        )
        assertEquals("https://example.com/webdav/", config.getNormalizedUrl())
    }

    // ========== getBaseUrl 测试 ==========

    @Test
    fun `getBaseUrl removes trailing slash`() {
        val config = ServerConfig(
            name = "Test",
            url = "https://example.com/webdav/"
        )
        assertEquals("https://example.com/webdav", config.getBaseUrl())
    }

    @Test
    fun `getBaseUrl returns same URL when no trailing slash`() {
        val config = ServerConfig(
            name = "Test",
            url = "https://example.com/webdav"
        )
        assertEquals("https://example.com/webdav", config.getBaseUrl())
    }

    // ========== requiresAuth 测试 ==========

    @Test
    fun `requiresAuth returns false when no credentials`() {
        val config = ServerConfig(
            name = "Test",
            url = "https://example.com",
            username = "",
            password = ""
        )
        assertFalse(config.requiresAuth())
    }

    @Test
    fun `requiresAuth returns true when username provided`() {
        val config = ServerConfig(
            name = "Test",
            url = "https://example.com",
            username = "user",
            password = ""
        )
        assertTrue(config.requiresAuth())
    }

    @Test
    fun `requiresAuth returns true when password provided`() {
        val config = ServerConfig(
            name = "Test",
            url = "https://example.com",
            username = "",
            password = "pass"
        )
        assertTrue(config.requiresAuth())
    }

    @Test
    fun `requiresAuth returns true when both credentials provided`() {
        val config = ServerConfig(
            name = "Test",
            url = "https://example.com",
            username = "user",
            password = "pass"
        )
        assertTrue(config.requiresAuth())
    }

    // ========== 数据类特性测试 ==========

    @Test
    fun `ServerConfig copy works correctly`() {
        val original = ServerConfig(
            name = "Original",
            url = "https://original.com"
        )
        val copied = original.copy(name = "Copied")
        assertEquals("Original", original.name)
        assertEquals("Copied", copied.name)
        assertEquals(original.url, copied.url)
    }

    @Test
    fun `ServerConfig equality works correctly`() {
        val id = "test-id"
        val config1 = ServerConfig(
            id = id,
            name = "Test",
            url = "https://example.com"
        )
        val config2 = ServerConfig(
            id = id,
            name = "Test",
            url = "https://example.com"
        )
        assertEquals(config1, config2)
    }
}
