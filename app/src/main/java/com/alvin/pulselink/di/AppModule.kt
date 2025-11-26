package com.alvin.pulselink.di

import android.content.Context
import com.alvin.pulselink.data.local.LocalDataSource
import com.alvin.pulselink.data.repository.AuthRepositoryImpl
import com.alvin.pulselink.data.repository.CaregiverRelationRepositoryImpl
import com.alvin.pulselink.data.repository.ChatRepositoryImpl
import com.alvin.pulselink.data.repository.HealthRecordRepositoryImpl
import com.alvin.pulselink.data.repository.HealthRepositoryImpl
import com.alvin.pulselink.data.repository.SeniorProfileRepositoryImpl
import com.alvin.pulselink.domain.repository.AuthRepository
import com.alvin.pulselink.domain.repository.CaregiverRelationRepository
import com.alvin.pulselink.domain.repository.ChatRepository
import com.alvin.pulselink.domain.repository.HealthRecordRepository
import com.alvin.pulselink.domain.repository.HealthRepository
import com.alvin.pulselink.domain.repository.SeniorProfileRepository
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
        val auth = FirebaseAuth.getInstance()
        
        // 在开发环境中禁用应用验证，避免 reCAPTCHA 问题
        // 注意：仅在调试模式下使用
        try {
            auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
            android.util.Log.d("AppModule", "Firebase Auth: App verification disabled for testing")
        } catch (e: Exception) {
            android.util.Log.w("AppModule", "Failed to disable app verification: ${e.message}")
        }
        
        return auth
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
     * 提供 ChatRepository 实现
     */
    @Provides
    @Singleton
    fun provideChatRepository(
        firestore: FirebaseFirestore,
        firebaseAuth: FirebaseAuth
    ): ChatRepository {
        return ChatRepositoryImpl(firestore, firebaseAuth)
    }
    
    // ========== Repository Providers ==========
    
    /**
     * 提供 SeniorProfileRepository 实现
     */
    @Provides
    @Singleton
    fun provideSeniorProfileRepository(
        firestore: FirebaseFirestore,
        functions: FirebaseFunctions
    ): SeniorProfileRepository {
        return SeniorProfileRepositoryImpl(firestore, functions)
    }
    
    /**
     * 提供 CaregiverRelationRepository 实现
     */
    @Provides
    @Singleton
    fun provideCaregiverRelationRepository(
        firestore: FirebaseFirestore
    ): CaregiverRelationRepository {
        return CaregiverRelationRepositoryImpl(firestore)
    }
    
    /**
     * 提供 HealthRecordRepository 实现
     */
    @Provides
    @Singleton
    fun provideHealthRecordRepository(
        firestore: FirebaseFirestore
    ): HealthRecordRepository {
        return HealthRecordRepositoryImpl(firestore)
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
