package com.alvin.pulselink.di

import android.content.Context
import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.data.repository.AuthRepositoryImpl
import com.alvin.pulselink.data.repository.HealthRepositoryImpl
import com.alvin.pulselink.data.repository.SeniorRepositoryImpl
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.HealthRepository
import com.alvin.pulselink.domain.repository.SeniorRepository
import com.alvin.pulselink.domain.usecase.LoginUseCase
import com.alvin.pulselink.domain.usecase.RegisterUseCase
import com.alvin.pulselink.domain.usecase.ResetPasswordUseCase
import com.alvin.pulselink.domain.usecase.TestFirestoreConnectionUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
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
    
    /**
     * 提供 Firebase Functions 实例
     */
    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions {
        return FirebaseFunctions.getInstance()
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
        firestore: FirebaseFirestore,
        localDataSource: LocalDataSource
    ): AuthRepository {
        return AuthRepositoryImpl(firebaseAuth, firestore, localDataSource)
    }
    
    /**
     * 提供 HealthRepository 实现
     */
    @Provides
    @Singleton
    fun provideHealthRepository(
        firestore: FirebaseFirestore,
        firebaseAuth: FirebaseAuth
    ): HealthRepository {
        return HealthRepositoryImpl(firestore, firebaseAuth)
    }
    
    /**
     * 提供 SeniorRepository 实现
     */
    @Provides
    @Singleton
    fun provideSeniorRepository(
        firestore: FirebaseFirestore
    ): SeniorRepository {
        return SeniorRepositoryImpl(firestore)
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
