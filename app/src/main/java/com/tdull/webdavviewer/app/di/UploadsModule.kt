package com.tdull.webdavviewer.app.di

import com.tdull.webdavviewer.app.data.repository.UploadsRepository
import com.tdull.webdavviewer.app.data.repository.UploadsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 上传功能依赖注入模块。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UploadsModule {

    @Binds
    @Singleton
    abstract fun bindUploadsRepository(
        impl: UploadsRepositoryImpl
    ): UploadsRepository
}
