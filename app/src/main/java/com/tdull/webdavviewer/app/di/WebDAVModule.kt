package com.tdull.webdavviewer.app.di

import android.content.Context
import com.tdull.webdavviewer.app.data.remote.WebDAVClient
import com.tdull.webdavviewer.app.data.repository.WebDAVRepository
import com.tdull.webdavviewer.app.data.repository.WebDAVRepositoryImpl
import com.tdull.webdavviewer.app.util.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * WebDAV模块的Hilt依赖注入配置
 */
@Module
@InstallIn(SingletonComponent::class)
object WebDAVModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        // 创建HTTP缓存目录
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, 50L * 1024 * 1024) // 50MB HTTP缓存
        
        return OkHttpClient.Builder()
            // 连接超时配置
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // 重试机制
            .retryOnConnectionFailure(true)
            // 缓存配置
            .cache(cache)
            // 日志拦截器（仅调试模式）
            .apply {
                if (com.tdull.webdavviewer.app.BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        }
                    )
                }
            }
            .build()
    }
    
    @Provides
    @Singleton
    fun provideWebDAVClient(okHttpClient: OkHttpClient): WebDAVClient {
        return WebDAVClient(okHttpClient)
    }
    
    @Module
    @InstallIn(SingletonComponent::class)
    abstract class WebDAVRepositoryModule {
        @Binds
        @Singleton
        abstract fun bindWebDAVRepository(
            impl: WebDAVRepositoryImpl
        ): WebDAVRepository
    }
}
