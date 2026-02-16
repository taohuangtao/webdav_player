package com.tdull.webdavviewer.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 播放器模块的Hilt依赖注入配置
 * 注意：ExoPlayer 实例由 ViewModel 自行创建和管理
 * 此模块可用于提供播放器相关的配置或工具类
 */
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    // ExoPlayer 由 VideoPlayerViewModel 自行创建和管理
    // 这样可以确保每个 ViewModel 有独立的播放器实例
    // 并且在 ViewModel 清除时正确释放资源
}
