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
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * WebDAV模块的Hilt依赖注入配置
 */
@Module
@InstallIn(SingletonComponent::class)
object WebDAVModule {
    
    /**
     * 提供WebDAV专用的OkHttpClient（不带认证拦截器）
     */
    @Provides
    @Singleton
    @Named("WebDAV")
    fun provideWebDAVOkHttpClient(
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
    
    /**
     * 提供WebDAV认证拦截器
     * 用于Coil图片加载时自动添加认证头部
     */
    @Provides
    @Singleton
    fun provideWebDAVAuthInterceptor(webDAVClient: WebDAVClient): WebDAVAuthInterceptor {
        return WebDAVAuthInterceptor(webDAVClient)
    }
    
    /**
     * 提供Coil专用的OkHttpClient（带WebDAV认证拦截器）
     */
    @Provides
    @Singleton
    @Named("Coil")
    fun provideCoilOkHttpClient(
        @ApplicationContext context: Context,
        authInterceptor: WebDAVAuthInterceptor
    ): OkHttpClient {
        val cacheDir = File(context.cacheDir, "coil_cache")
        val cache = Cache(cacheDir, 50L * 1024 * 1024) // 50MB缓存
        
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .cache(cache)
            .addInterceptor(authInterceptor)
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
    fun provideWebDAVClient(@Named("WebDAV") okHttpClient: OkHttpClient): WebDAVClient {
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

/**
 * WebDAV认证拦截器
 * 自动为WebDAV服务器请求添加认证头部
 */
class WebDAVAuthInterceptor(
    private val webDAVClient: WebDAVClient
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val config = webDAVClient.getCurrentConfig()
        
        // 如果有服务器配置且需要认证，添加认证头部
        val newRequest = if (config != null && config.requiresAuth()) {
            val credentials = Credentials.basic(config.username, config.password)
            request.newBuilder()
                .header("Authorization", credentials)
                .build()
        } else {
            request
        }
        
        return chain.proceed(newRequest)
    }
}
