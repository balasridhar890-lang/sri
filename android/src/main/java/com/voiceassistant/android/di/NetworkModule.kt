package com.voiceassistant.android.di

import com.voiceassistant.android.config.AppConfig
import com.voiceassistant.android.datastore.AppPreferences
import com.voiceassistant.android.network.BackendClient
import com.voiceassistant.android.repository.PreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for network and repository dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Singleton
    @Provides
    fun provideBackendClient(config: AppConfig): BackendClient {
        return BackendClient(config)
    }
    
    @Singleton
    @Provides
    fun providePreferencesRepository(
        preferences: AppPreferences,
        backendClient: BackendClient
    ): PreferencesRepository {
        return PreferencesRepository(preferences, backendClient)
    }
}
