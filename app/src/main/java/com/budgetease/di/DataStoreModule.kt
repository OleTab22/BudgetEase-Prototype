package com.budgetease.di

import android.content.Context
import com.budgetease.data.preferences.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Dagger Hilt module for DataStore-related dependency injection
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    // Provides singleton instance of settings repository for app preferences
    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepository(context)
    }
} 