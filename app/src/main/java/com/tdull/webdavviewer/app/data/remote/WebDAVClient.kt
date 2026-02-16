package com.tdull.webdavviewer.app.data.remote

import android.util.Log
import com.tdull.webdavviewer.app.data.model.ResourceType
import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.data.model.WebDAVException
import com.tdull.webdavviewer.app.data.model.WebDAVResource
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 服务器类型枚举
 */
enum class ServerType {
    PROPFIND,    // 标准WebDAV，支持PROPFIND请求
    AUTOINDEX    // Nginx autoindex，需解析HTML目录列表
}

/**
 * WebDAV客户端，支持两种模式：
 * 1. 标准WebDAV PROPFIND请求
 * 2. Nginx autoindex HTML目录列表解析
 */
@Singleton
class WebDAVClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private var currentConfig: ServerConfig? = null
    private var serverType: ServerType? = null  // 缓存服务器类型
    
    companion object {
        private const val TAG = "WebDAVClient"
        private const val PROPFIND_DEPTH_HEADER = "Depth"
        private const val PROPFIND_DEPTH_ONE = "1"
        private const val PROPFIND_CONTENT_TYPE = "application/xml; charset=utf-8"
        
        // PROPFIND请求体
        private val PROPFIND_BODY = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:propfind xmlns:d="DAV:">
                <d:prop>
                    <d:displayname/>
                    <d:resourcetype/>
                    <d:getcontentlength/>
                    <d:getlastmodified/>
                    <d:getcontenttype/>
                </d:prop>
            </d:propfind>
        """.trimIndent()
        
        // Nginx autoindex HTML解析正则
        // 匹配 <a href="...">...</a> 后跟日期和大小
        private val AUTOINDEX_LINK_PATTERN = Pattern.compile(
            """<a\s+href="([^"]+)"[^>]*>([^<]+)</a>""",
            Pattern.CASE_INSENSITIVE
        )
        
        // 日期大小行正则：匹配 "dd-MMM-yyyy HH:mm    size" 或 "dd-MMM-yyyy HH:mm    -"
        private val AUTOINDEX_INFO_PATTERN = Pattern.compile(
            """(\d{1,2}-\w{3}-\d{4}\s+\d{2}:\d{2})\s+(-|\d+)""",
            Pattern.CASE_INSENSITIVE
        )
        
        // 日期格式: 16-Feb-2026 10:00
        private val AUTOINDEX_DATE_FORMATS = listOf(
            "dd-MMM-yyyy HH:mm",
            "dd-MMM-yyyy HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "dd MMM yyyy HH:mm"
        )
    }
    
    /**
     * 连接到WebDAV服务器
     */
    fun connect(config: ServerConfig): Boolean {
        currentConfig = config
        serverType = null  // 重置服务器类型缓存
        return testConnection()
    }
    
    /**
     * 测试连接
     */
    fun testConnection(): Boolean {
        val config = currentConfig ?: throw IllegalStateException("未配置服务器")
        return testConnection(config)
    }
    
    /**
     * 测试指定配置的连接
     */
    fun testConnection(config: ServerConfig): Boolean {
        val url = config.getNormalizedUrl()
        
        // 首先尝试PROPFIND
        val propfindRequest = buildPropfindRequest(url, config, depth = "0")
        
        return try {
            val response = okHttpClient.newCall(propfindRequest).execute()
            if (response.isSuccessful || response.code == 207) {
                serverType = ServerType.PROPFIND
                Log.d(TAG, "服务器支持PROPFIND: ${response.code}")
                true
            } else if (response.code == 405 || response.code == 400 || response.code == 403) {
                // PROPFIND不支持，尝试GET请求获取目录列表
                tryGetAutoindex(url, config)
            } else {
                Log.w(TAG, "PROPFIND响应: ${response.code}")
                // 尝试autoindex
                tryGetAutoindex(url, config)
            }
        } catch (e: Exception) {
            Log.w(TAG, "PROPFIND失败: ${e.message}")
            // 尝试autoindex
            tryGetAutoindex(url, config)
        }
    }
    
    /**
     * 尝试获取autoindex目录列表
     */
    private fun tryGetAutoindex(url: String, config: ServerConfig): Boolean {
        val getRequest = buildGetRequest(url, config)
        
        return try {
            val response = okHttpClient.newCall(getRequest).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                // 检查是否是HTML目录列表
                if (isHtmlDirectoryListing(body)) {
                    serverType = ServerType.AUTOINDEX
                    Log.d(TAG, "服务器使用autoindex模式")
                    true
                } else {
                    Log.w(TAG, "响应不是目录列表格式")
                    false
                }
            } else {
                Log.w(TAG, "GET请求失败: ${response.code}")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "GET请求异常: ${e.message}")
            false
        }
    }
    
    /**
     * 检测HTML是否为目录列表
     */
    private fun isHtmlDirectoryListing(html: String): Boolean {
        val lowerHtml = html.lowercase()
        return (lowerHtml.contains("<title>index of") || 
                lowerHtml.contains("<h1>index of") ||
                lowerHtml.contains("directory listing")) &&
               lowerHtml.contains("<a href=")
    }
    
    /**
     * 设置服务器类型（手动指定）
     */
    fun setServerType(type: ServerType) {
        this.serverType = type
    }
    
    /**
     * 获取当前服务器类型
     */
    fun getServerType(): ServerType? = serverType
    
    /**
     * 列出指定路径下的文件和目录
     */
    fun listFiles(path: String): List<WebDAVResource> {
        val config = currentConfig ?: throw IllegalStateException("未配置服务器")
        val detectedType = serverType ?: detectServerType(config)
        
        return when (detectedType) {
            ServerType.PROPFIND -> listFilesViaPropfind(config, path)
            ServerType.AUTOINDEX -> listFilesViaAutoindex(config, path)
        }
    }
    
    /**
     * 自动检测服务器类型
     */
    private fun detectServerType(config: ServerConfig): ServerType {
        val url = config.getNormalizedUrl()
        
        // 先尝试PROPFIND
        val propfindRequest = buildPropfindRequest(url, config, depth = "0")
        try {
            val response = okHttpClient.newCall(propfindRequest).execute()
            if (response.isSuccessful || response.code == 207) {
                serverType = ServerType.PROPFIND
                return ServerType.PROPFIND
            }
        } catch (e: Exception) {
            Log.d(TAG, "PROPFIND检测失败: ${e.message}")
        }
        
        // 检测为autoindex
        serverType = ServerType.AUTOINDEX
        return ServerType.AUTOINDEX
    }
    
    /**
     * 通过PROPFIND请求获取文件列表（标准WebDAV）
     */
    private fun listFilesViaPropfind(config: ServerConfig, path: String): List<WebDAVResource> {
        val url = buildFullUrl(config, path)
        val request = buildPropfindRequest(url, config, depth = PROPFIND_DEPTH_ONE)
        
        val response = try {
            okHttpClient.newCall(request).execute()
        } catch (e: Exception) {
            throw WebDAVException.ConnectionFailed(e)
        }
        
        if (!response.isSuccessful && response.code != 207) {
            handleErrorResponse(response)
        }
        
        val responseBody = response.body?.string()
            ?: throw WebDAVException.InvalidResponse("响应体为空")
        
        return parseMultistatusResponse(responseBody, config.getBaseUrl())
    }
    
    /**
     * 通过GET请求解析HTML目录列表（Nginx autoindex）
     */
    private fun listFilesViaAutoindex(config: ServerConfig, path: String): List<WebDAVResource> {
        val url = buildFullUrl(config, path)
        val request = buildGetRequest(url, config)
        
        val response = try {
            okHttpClient.newCall(request).execute()
        } catch (e: Exception) {
            throw WebDAVException.ConnectionFailed(e)
        }
        
        if (!response.isSuccessful) {
            handleErrorResponse(response)
        }
        
        val responseBody = response.body?.string()
            ?: throw WebDAVException.InvalidResponse("响应体为空")
        
        return parseHtmlDirectoryListing(responseBody, path)
    }
    
    /**
     * 获取文件的流媒体URL
     */
    fun getStreamUrl(path: String): String {
        val config = currentConfig ?: throw IllegalStateException("未配置服务器")
        return buildFullUrl(config, path)
    }
    
    /**
     * 构建完整的URL
     */
    private fun buildFullUrl(config: ServerConfig, path: String): String {
        val baseUrl = config.getNormalizedUrl()
        val normalizedPath = if (path.startsWith("/")) path.substring(1) else path
        // 确保路径以/结尾（目录请求）
        val fullPath = if (normalizedPath.isNotEmpty() && !normalizedPath.endsWith("/")) {
            "$baseUrl${normalizedPath.encodePath()}/"
        } else {
            "$baseUrl${normalizedPath.encodePath()}"
        }
        return fullPath
    }
    
    /**
     * 构建PROPFIND请求
     */
    private fun buildPropfindRequest(
        url: String,
        config: ServerConfig,
        depth: String = PROPFIND_DEPTH_ONE
    ): Request {
        val requestBuilder = Request.Builder()
            .url(url)
            .method("PROPFIND", PROPFIND_BODY.toRequestBody(PROPFIND_CONTENT_TYPE.toMediaTypeOrNull()))
            .header(PROPFIND_DEPTH_HEADER, depth)
        
        if (config.requiresAuth()) {
            val credentials = Credentials.basic(config.username, config.password)
            requestBuilder.header("Authorization", credentials)
        }
        
        return requestBuilder.build()
    }
    
    /**
     * 构建GET请求
     */
    private fun buildGetRequest(url: String, config: ServerConfig): Request {
        val requestBuilder = Request.Builder().url(url)
        
        if (config.requiresAuth()) {
            val credentials = Credentials.basic(config.username, config.password)
            requestBuilder.header("Authorization", credentials)
        }
        
        return requestBuilder.build()
    }
    
    /**
     * 处理错误响应
     */
    private fun handleErrorResponse(response: Response): Nothing {
        when (response.code) {
            401 -> throw WebDAVException.AuthenticationFailed()
            404 -> throw WebDAVException.ResourceNotFound("请求的资源")
            in 500..599 -> throw WebDAVException.ServerError(response.code, response.message)
            else -> throw WebDAVException.ServerError(response.code, response.message)
        }
    }
    
    /**
     * 解析WebDAV multistatus XML响应
     */
    private fun parseMultistatusResponse(xml: String, baseUrl: String): List<WebDAVResource> {
        val resources = mutableListOf<WebDAVResource>()
        
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            
            var currentPath: String? = null
            var currentName: String? = null
            var isDirectory = false
            var currentSize: Long = 0
            var currentLastModified: Long = 0
            var currentContentType: String? = null
            var isInProp = false
            var currentTag = ""
            
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        when {
                            parser.name == "response" && parser.namespace == "DAV:" -> {
                                currentPath = null
                                currentName = null
                                isDirectory = false
                                currentSize = 0
                                currentLastModified = 0
                                currentContentType = null
                            }
                            parser.name == "prop" && parser.namespace == "DAV:" -> {
                                isInProp = true
                            }
                            parser.name == "collection" && parser.namespace == "DAV:" -> {
                                isDirectory = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim() ?: ""
                        when {
                            currentTag == "href" && currentPath == null -> {
                                currentPath = URLDecoder.decode(text, StandardCharsets.UTF_8.name())
                            }
                            currentTag == "displayname" && isInProp -> {
                                currentName = text
                            }
                            currentTag == "getcontentlength" && isInProp -> {
                                currentSize = text.toLongOrNull() ?: 0
                            }
                            currentTag == "getlastmodified" && isInProp -> {
                                currentLastModified = parseHttpDate(text)
                            }
                            currentTag == "getcontenttype" && isInProp -> {
                                currentContentType = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when {
                            parser.name == "prop" && parser.namespace == "DAV:" -> {
                                isInProp = false
                            }
                            parser.name == "response" && parser.namespace == "DAV:" -> {
                                val path = currentPath
                                if (path != null) {
                                    val name = currentName ?: path.substringAfterLast('/').ifEmpty { path }
                                    
                                    val normalizedPath = path.trim('/')
                                    val normalizedBase = baseUrl.substringAfter("://").substringAfter('/')
                                    if (normalizedPath != normalizedBase.trim('/') && normalizedPath.isNotEmpty()) {
                                        val displayName = URLDecoder.decode(name, StandardCharsets.UTF_8.name())
                                        val resourceType = WebDAVResource.determineResourceType(
                                            displayName, currentContentType, isDirectory
                                        )
                                        
                                        resources.add(
                                            WebDAVResource(
                                                path = path,
                                                name = displayName,
                                                isDirectory = isDirectory,
                                                size = currentSize,
                                                lastModified = currentLastModified,
                                                contentType = currentContentType,
                                                resourceType = resourceType
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            throw WebDAVException.InvalidResponse("XML解析失败: ${e.message}")
        }
        
        return resources.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }
    
    /**
     * 解析HTML目录列表（Nginx autoindex格式）
     * 格式：<a href="filename">name</a>
     *       日期时间 大小
     */
    private fun parseHtmlDirectoryListing(html: String, currentPath: String): List<WebDAVResource> {
        val resources = mutableListOf<WebDAVResource>()
        val linkMatcher = AUTOINDEX_LINK_PATTERN.matcher(html)
        
        while (linkMatcher.find()) {
            try {
                val href = linkMatcher.group(1) ?: continue
                val name = linkMatcher.group(2) ?: continue
                
                // 跳过父目录链接
                if (href == "../" || href == ".." || name == "Parent Directory") {
                    continue
                }
                
                // 在 <a> 标签后面查找日期和大小信息
                val searchStart = linkMatcher.end()
                val searchEnd = minOf(searchStart + 300, html.length)
                val searchText = html.substring(searchStart, searchEnd)
                
                val infoMatcher = AUTOINDEX_INFO_PATTERN.matcher(searchText)
                var dateStr = ""
                var sizeStr = "-"
                
                if (infoMatcher.find()) {
                    dateStr = infoMatcher.group(1) ?: ""
                    sizeStr = infoMatcher.group(2) ?: "-"
                }
                
                Log.d(TAG, "解析: href=$href, name=$name, date=$dateStr, size=$sizeStr")
                
                // 判断是否为目录
                val isDirectory = href.endsWith("/")
                
                // 解析大小
                val size = parseAutoindexSize(sizeStr)
                
                // 解析日期
                val lastModified = parseAutoindexDate(dateStr)
                
                // URL解码文件名
                val decodedName = URLDecoder.decode(name, StandardCharsets.UTF_8.name())
                    .trimEnd('/')
                
                // 构建完整路径
                val normalizedPath = if (currentPath.endsWith("/")) {
                    "$currentPath$href"
                } else {
                    "$currentPath/$href"
                }
                
                // 确定资源类型
                val resourceType = WebDAVResource.determineResourceType(
                    decodedName, null, isDirectory
                )
                
                resources.add(
                    WebDAVResource(
                        path = normalizedPath,
                        name = decodedName,
                        isDirectory = isDirectory,
                        size = size,
                        lastModified = lastModified,
                        contentType = null,
                        resourceType = resourceType
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "解析HTML链接失败: ${e.message}")
            }
        }
        
        return resources.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }
    
    /**
     * 解析autoindex文件大小
     * 格式: 123456, 1.2K, 1.5M, 2.0G, 或 "-" 表示目录
     */
    private fun parseAutoindexSize(sizeStr: String): Long {
        if (sizeStr == "-" || sizeStr.isEmpty()) return 0
        
        return try {
            val lower = sizeStr.lowercase()
            when {
                lower.endsWith("k") -> (lower.dropLast(1).toDouble() * 1024).toLong()
                lower.endsWith("m") -> (lower.dropLast(1).toDouble() * 1024 * 1024).toLong()
                lower.endsWith("g") -> (lower.dropLast(1).toDouble() * 1024 * 1024 * 1024).toLong()
                else -> lower.toLongOrNull() ?: 0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 解析autoindex日期
     * 格式: 16-Feb-2026 10:00
     */
    private fun parseAutoindexDate(dateStr: String): Long {
        if (dateStr.isEmpty()) return 0
        
        for (format in AUTOINDEX_DATE_FORMATS) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("GMT")
                return sdf.parse(dateStr)?.time ?: 0
            } catch (e: Exception) {
                continue
            }
        }
        return 0
    }
    
    /**
     * 解析HTTP日期格式
     */
    private fun parseHttpDate(dateString: String): Long {
        return try {
            val formats = listOf(
                "EEE, dd MMM yyyy HH:mm:ss zzz",
                "EEE, dd-MMM-yyyy HH:mm:ss zzz",
                "EEE, dd MMM yyyy HH:mm:ss Z"
            )
            
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("GMT")
                    return sdf.parse(dateString)?.time ?: 0
                } catch (e: Exception) {
                    continue
                }
            }
            0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * URL路径编码
     */
    private fun String.encodePath(): String {
        return this.split("/").joinToString("/") { segment ->
            java.net.URLEncoder.encode(segment, StandardCharsets.UTF_8.name())
                .replace("+", "%20")
                .replace("%2F", "/")
        }
    }
}

/**
 * 字符串转MediaType
 */
private fun String.toMediaTypeOrNull(): okhttp3.MediaType? {
    return try {
        this.toMediaType()
    } catch (e: Exception) {
        null
    }
}
