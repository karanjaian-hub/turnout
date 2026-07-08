package com.turnout.android.core.di

import com.turnout.android.data.repository.AuthRepositoryImpl
import com.turnout.android.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// @Binds is more efficient than @Provides for interface→impl wiring —
// no wrapper function body needed, Hilt handles it at compile time
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
