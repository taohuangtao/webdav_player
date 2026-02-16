package com.tdull.webdavviewer.app.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.tdull.webdavviewer.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Coil图片加载模块配置
 * 提供优化的ImageLoader实例
 */
@Module
@InstallIn(SingletonComponent::class)
object CoilModule {

    /**
     * 提供优化的Coil ImageLoader
     */
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader {
        return ImageLoader.Builder(context)
            // 使用内存缓存
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // 使用25%的应用内存作为图片缓存
                    .build()
            }
            // 使用磁盘缓存
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512L * 1024 * 1024) // 512MB磁盘缓存
                    .build()
            }
            // 网络配置
            .okHttpClient(okHttpClient)
            // 缓存策略
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // 交叉淡入动画
            .crossfade(true)
            .crossfade(300)
            // 错误处理
            .respectCacheHeaders(false) // 忽略服务器缓存头，使用本地缓存策略
            // 日志（仅在调试模式）
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}
