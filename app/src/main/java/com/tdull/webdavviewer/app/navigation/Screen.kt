package com.tdull.webdavviewer.app.navigation

/**
 * 应用导航路由定义
 */
sealed class Screen(val route: String) {
    /**
     * 设置页面 - 服务器配置管理
     */
    object Settings : Screen("settings")

    /**
     * 文件浏览器页面
     * @param serverId 服务器ID
     */
    object Browser : Screen("browser?serverId={serverId}") {
        fun createRoute(serverId: String) = "browser?serverId=$serverId"
    }

    /**
     * 视频播放器页面
     * @param url 视频URL（需编码）
     * @param title 视频标题（需编码）
     */
    object VideoPlayer : Screen("video?url={url}&title={title}") {
        fun createRoute(url: String, title: String = ""): String {
            val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            return "video?url=$encodedUrl&title=$encodedTitle"
        }
    }

    /**
     * 图片查看器页面
     * @param url 图片URL（需编码）
     * @param title 图片标题（需编码）
     */
    object ImageViewer : Screen("image?url={url}&title={title}") {
        fun createRoute(url: String, title: String = ""): String {
            val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            return "image?url=$encodedUrl&title=$encodedTitle"
        }
    }

    /**
     * 收藏列表页面
     */
    object Favorites : Screen("favorites")

    /**
     * 下载列表页面
     */
    object Downloads : Screen("downloads")
}
