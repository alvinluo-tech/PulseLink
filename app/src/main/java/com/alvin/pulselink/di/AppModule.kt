package com.alvin.pulselink.di

import android.content.Context
import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.data.repository.AuthRepositoryImpl
import com.alvin.pulselink.data.repository.HealthRepositoryImpl
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.HealthRepository
import com.alvin.pulselink.domain.usecase.LoginUseCase
import com.alvin.pulselink.domain.usecase.RegisterUseCase
import com.alvin.pulselink.domain.usecase.ResetPasswordUseCase
import com.alvin.pulselink.domain.usecase.TestFirestoreConnectionUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    /**
     * 提供 Firebase Auth 实例
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    /**
     * 提供 Firebase Firestore 实例
     */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideLocalDataSource(
        @ApplicationContext context: Context
    ): LocalDataSource {
        return LocalDataSource(context)
    }
    
    /**
     * 提供 AuthRepository 实现
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository {
        return AuthRepositoryImpl(firebaseAuth, firestore)
    }
    
    /**
     * 提供 HealthRepository 实现
     */
    @Provides
    @Singleton
    fun provideHealthRepository(
        firestore: FirebaseFirestore
    ): HealthRepository {
        return HealthRepositoryImpl(firestore)
    }
    
    @Provides
    fun provideLoginUseCase(
        authRepository: AuthRepository
    ): LoginUseCase {
        return LoginUseCase(authRepository)
    }
    
    @Provides
    fun provideRegisterUseCase(
        authRepository: AuthRepository
    ): RegisterUseCase {
        return RegisterUseCase(authRepository)
    }
    
    @Provides
    fun provideResetPasswordUseCase(
        authRepository: AuthRepository
    ): ResetPasswordUseCase {
        return ResetPasswordUseCase(authRepository)
    }
    
    @Provides
    fun provideTestFirestoreConnectionUseCase(
        healthRepository: HealthRepository
    ): TestFirestoreConnectionUseCase {
        return TestFirestoreConnectionUseCase(healthRepository)
    }
}
