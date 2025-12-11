package com.voiceassistant.android.di

import com.voiceassistant.android.config.AppConfig
import com.voiceassistant.android.network.BackendClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for network dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Singleton
    @Provides
    fun provideBackendClient(config: AppConfig): BackendClient {
        return BackendClient(config)
    }
}
