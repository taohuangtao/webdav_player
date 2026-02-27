package com.tdull.webdavviewer.app.data.model

import java.util.UUID

/**
 * 收藏项数据类
 * 用于存储用户收藏的视频信息
 */
data class FavoriteItem(
    val id: String = UUID.randomUUID().toString(),
    val videoUrl: String,
    val videoTitle: String,
    val serverId: String,  // 关联服务器，用于获取认证信息
    val resourcePath: String,  // WebDAV 资源路径，用于加载预览图
    val addedAt: Long = System.currentTimeMillis()
)
