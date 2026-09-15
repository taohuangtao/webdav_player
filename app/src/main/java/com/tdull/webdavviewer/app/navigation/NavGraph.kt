package com.tdull.webdavviewer.app.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tdull.webdavviewer.app.ui.browser.FileBrowserScreen
import com.tdull.webdavviewer.app.ui.player.VideoPlayerScreen
import com.tdull.webdavviewer.app.ui.settings.SettingsScreen
import com.tdull.webdavviewer.app.ui.viewer.ImageViewerScreen
import com.tdull.webdavviewer.app.ui.viewer.ImageViewerItem
import com.tdull.webdavviewer.app.ui.favorites.FavoritesScreen
import com.tdull.webdavviewer.app.ui.downloads.DownloadsScreen
import java.net.URLDecoder

private const val IMAGE_VIEWER_URLS_KEY = "imageViewerUrls"
private const val IMAGE_VIEWER_THUMBNAIL_URLS_KEY = "imageViewerThumbnailUrls"
private const val IMAGE_VIEWER_TITLES_KEY = "imageViewerTitles"
private const val IMAGE_VIEWER_INITIAL_INDEX_KEY = "imageViewerInitialIndex"

/**
 * 应用导航图
 * 定义所有页面之间的导航关系
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Settings.route,
        modifier = modifier
    ) {
        // 设置页面
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateToBrowser = { serverId ->
                    navController.navigate(Screen.Browser.createRoute(serverId))
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route)
                },
                onNavigateToDownloads = {
                    navController.navigate(Screen.Downloads.route)
                }
            )
        }

        // 文件浏览器页面
        composable(
            route = Screen.Browser.route,
            arguments = listOf(
                navArgument("serverId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getString("serverId")
            FileBrowserScreen(
                serverId = serverId,
                onVideoClick = { url ->
                    navController.navigate(Screen.VideoPlayer.createRoute(url))
                },
                onImageClick = { items, initialIndex ->
                    val selectedItem = items.getOrNull(initialIndex) ?: items.firstOrNull()
                    if (selectedItem != null) {
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            set(IMAGE_VIEWER_URLS_KEY, ArrayList(items.map { it.url }))
                            set(IMAGE_VIEWER_THUMBNAIL_URLS_KEY, ArrayList(items.map { it.mediumThumbnailUrl }))
                            set(IMAGE_VIEWER_TITLES_KEY, ArrayList(items.map { it.title }))
                            set(IMAGE_VIEWER_INITIAL_INDEX_KEY, initialIndex.coerceIn(0, items.lastIndex))
                        }
                        navController.navigate(
                            Screen.ImageViewer.createRoute(
                                url = selectedItem.url,
                                title = selectedItem.title,
                                thumbnailUrl = selectedItem.mediumThumbnailUrl
                            )
                        )
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 视频播放器页面
        composable(
            route = Screen.VideoPlayer.route,
            arguments = listOf(
                navArgument("url") {
                    type = NavType.StringType
                },
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""

            // 解码URL参数
            val videoUrl = try {
                URLDecoder.decode(encodedUrl, "UTF-8")
            } catch (e: Exception) {
                Uri.decode(encodedUrl)
            }
            val videoTitle = try {
                URLDecoder.decode(encodedTitle, "UTF-8")
            } catch (e: Exception) {
                Uri.decode(encodedTitle)
            }

            VideoPlayerScreen(
                videoUrl = videoUrl,
                videoTitle = videoTitle,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 图片查看器页面
        composable(
            route = Screen.ImageViewer.route,
            arguments = listOf(
                navArgument("url") {
                    type = NavType.StringType
                },
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("thumbnailUrl") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val imageUrl = backStackEntry.arguments?.getString("url") ?: ""
            val imageTitle = backStackEntry.arguments?.getString("title") ?: ""
            val mediumThumbnailUrl = backStackEntry.arguments?.getString("thumbnailUrl") ?: ""
            val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
            val imageUrls = savedStateHandle?.get<ArrayList<String>>(IMAGE_VIEWER_URLS_KEY).orEmpty()
            val thumbnailUrls = savedStateHandle
                ?.get<ArrayList<String>>(IMAGE_VIEWER_THUMBNAIL_URLS_KEY)
                .orEmpty()
            val imageTitles = savedStateHandle?.get<ArrayList<String>>(IMAGE_VIEWER_TITLES_KEY).orEmpty()
            val imageItems = imageUrls.mapIndexed { index, url ->
                ImageViewerItem(
                    url = url,
                    mediumThumbnailUrl = thumbnailUrls.getOrNull(index).orEmpty(),
                    title = imageTitles.getOrNull(index).orEmpty()
                )
            }
            val initialIndex = savedStateHandle?.get<Int>(IMAGE_VIEWER_INITIAL_INDEX_KEY) ?: 0

            ImageViewerScreen(
                imageUrl = imageUrl,
                mediumThumbnailUrl = mediumThumbnailUrl,
                imageTitle = imageTitle,
                items = imageItems,
                initialIndex = initialIndex,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 收藏列表页面
        composable(route = Screen.Favorites.route) {
            FavoritesScreen(
                onVideoClick = { url ->
                    navController.navigate(Screen.VideoPlayer.createRoute(url))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 下载列表页面
        composable(route = Screen.Downloads.route) {
            DownloadsScreen(
                onVideoClick = { url ->
                    navController.navigate(Screen.VideoPlayer.createRoute(url))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
