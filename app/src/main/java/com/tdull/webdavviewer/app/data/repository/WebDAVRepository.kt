package com.tdull.webdavviewer.app.data.repository

import com.tdull.webdavviewer.app.data.model.ServerConfig
import com.tdull.webdavviewer.app.data.model.WebDAVResource

/**
 * WebDAV数据仓库接口
 */
interface WebDAVRepository {
    /**
     * 连接到WebDAV服务器
     * @param config 服务器配置
     * @return 连接结果
     */
    suspend fun connect(config: ServerConfig): Result<Unit>
    
    /**
     * 列出指定路径下的文件和目录
     * @param path 目录路径
     * @return 资源列表
     */
    suspend fun listFiles(path: String): Result<List<WebDAVResource>>
    
    /**
     * 获取文件的流媒体URL
     * @param path 文件路径
     * @return 流媒体URL
     */
    fun getStreamUrl(path: String): String
    
    /**
     * 测试服务器连接
     * @param config 服务器配置
     * @return 连接测试结果
     */
    suspend fun testConnection(config: ServerConfig): Result<Boolean>
    
    /**
     * 获取视频预览图列表
     * @param videoPath 视频文件路径
     * @return 预览图URL列表
     */
    suspend fun getVideoPreviews(videoPath: String): Result<List<String>>
    
    /**
     * 重命名文件或文件夹
     * @param resource 要重命名的资源
     * @param newName 新的名称
     * @return 操作结果
     */
    suspend fun rename(resource: WebDAVResource, newName: String): Result<Unit>
    
    /**
     * 移动文件或文件夹到目标目录
     * @param resource 要移动的资源
     * @param destinationDir 目标目录路径
     * @return 操作结果
     */
    suspend fun move(resource: WebDAVResource, destinationDir: String): Result<Unit>
    
    /**
     * 删除文件或文件夹
     * @param resource 要删除的资源
     * @return 操作结果
     */
    suspend fun delete(resource: WebDAVResource): Result<Unit>
}
