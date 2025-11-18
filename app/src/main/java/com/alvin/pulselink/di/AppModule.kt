package com.alvin.pulselink.di

import android.content.Context
import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.data.repository.AuthRepositoryImpl
import com.alvin.pulselink.data.repository.HealthRepositoryImpl
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.HealthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideLocalDataSource(
        @ApplicationContext context: Context
    ): LocalDataSource {
        return LocalDataSource(context)
    }
    
    @Provides
    @Singleton
    fun provideAuthRepository(
        localDataSource: LocalDataSource
    ): AuthRepository {
        return AuthRepositoryImpl(localDataSource)
    }
    
    @Provides
    @Singleton
    fun provideHealthRepository(): HealthRepository {
        return HealthRepositoryImpl()
    }
}
