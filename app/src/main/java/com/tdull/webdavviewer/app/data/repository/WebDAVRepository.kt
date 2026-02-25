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
}
