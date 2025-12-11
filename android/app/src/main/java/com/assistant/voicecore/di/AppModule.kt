package com.assistant.voicecore.di

import android.content.Context
import com.assistant.voicecore.network.ApiService
import com.assistant.voicecore.repository.ConversationRepository
import com.assistant.voicecore.service.CallAnsweringService
import com.assistant.voicecore.service.CallScreeningService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Dagger/Hilt module for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideConversationRepository(apiService: ApiService): ConversationRepository {
        return ConversationRepository(apiService)
    }

    @Provides
    @Singleton
    fun provideCallAnsweringService(@ApplicationContext context: Context): CallAnsweringService {
        return CallAnsweringService(context)
    }

    @Provides
    @Singleton
    fun provideCallScreeningService(@ApplicationContext context: Context): CallScreeningService {
        return CallScreeningService(context)
    }
}