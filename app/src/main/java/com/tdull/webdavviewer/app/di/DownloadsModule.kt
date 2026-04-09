package com.tdull.webdavviewer.app.di

import com.tdull.webdavviewer.app.data.repository.DownloadsRepository
import com.tdull.webdavviewer.app.data.repository.DownloadsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 下载功能依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadsModule {

    @Binds
    @Singleton
    abstract fun bindDownloadsRepository(
        impl: DownloadsRepositoryImpl
    ): DownloadsRepository
}
