package com.tdull.webdavviewer.app

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WebDavApplication : Application(), ImageLoaderFactory {
    
    companion object {
        private const val TAG = "WebDavApplication"
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreate() {
        super.onCreate()
        
        // 设置全局异常处理器
        setupUncaughtExceptionHandler()
    }
    
    /**
     * 提供 Coil 全局 ImageLoader
     * 使用 Hilt 注入的带认证拦截器的 ImageLoader
     */
    override fun newImageLoader(): ImageLoader {
        return imageLoader
    }
    
    /**
     * 设置全局未捕获异常处理器
     * 用于记录异常并提供友好的崩溃提示
     */
    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // 记录异常日志
            Log.e(TAG, "Uncaught exception in thread: ${thread.name}", throwable)
            
            // 这里可以:
            // 1. 将异常信息保存到本地文件
            // 2. 上传到崩溃报告服务
            // 3. 显示友好的崩溃提示界面
            
            // 保存崩溃日志到本地（可选）
            saveCrashLog(throwable)
            
            // 调用默认处理器（通常会导致应用退出）
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    /**
     * 保存崩溃日志到本地
     */
    private fun saveCrashLog(throwable: Throwable) {
        try {
            val crashFile = java.io.File(cacheDir, "crash_log_${System.currentTimeMillis()}.txt")
            java.io.PrintWriter(crashFile).use { writer ->
                writer.println("=== Crash Log ===")
                writer.println("Time: ${java.util.Date()}")
                writer.println("Exception: ${throwable.javaClass.name}")
                writer.println("Message: ${throwable.message}")
                writer.println()
                writer.println("Stack Trace:")
                throwable.printStackTrace(writer)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log", e)
        }
    }
}
