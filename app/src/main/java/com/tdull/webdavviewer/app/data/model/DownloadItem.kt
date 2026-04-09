package com.tdull.webdavviewer.app.data.model

import java.util.UUID

/**
 * 下载项数据模型
 * 用于记录已下载的视频文件信息
 */
data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val videoUrl: String,           // 原始 WebDAV URL
    val videoTitle: String,         // 视频标题
    val serverId: String,           // 关联服务器ID
    val resourcePath: String,       // WebDAV 资源路径
    val localPath: String,          // 本地存储路径
    val fileSize: Long,             // 文件大小（字节）
    val downloadedAt: Long = System.currentTimeMillis()  // 下载完成时间
)
