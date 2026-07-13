package com.turnout.android.core.di

import com.turnout.android.data.repository.AuthRepositoryImpl
import com.turnout.android.data.repository.AiRepositoryImpl
import com.turnout.android.data.repository.DashboardRepositoryImpl
import com.turnout.android.data.repository.EmailRepositoryImpl
import com.turnout.android.data.repository.EventRepositoryImpl
import com.turnout.android.data.repository.GuestRepositoryImpl
import com.turnout.android.data.repository.RsvpRepositoryImpl
import com.turnout.android.data.repository.PaymentRepositoryImpl
import com.turnout.android.domain.repository.AiRepository
import com.turnout.android.domain.repository.AuthRepository
import com.turnout.android.domain.repository.DashboardRepository
import com.turnout.android.domain.repository.EmailRepository
import com.turnout.android.domain.repository.EventRepository
import com.turnout.android.domain.repository.GuestRepository
import com.turnout.android.domain.repository.RsvpRepository
import com.turnout.android.domain.repository.PaymentRepository
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

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository

    @Binds
    @Singleton
    abstract fun bindEmailRepository(impl: EmailRepositoryImpl): EmailRepository

    @Binds
    @Singleton
    abstract fun bindGuestRepository(impl: GuestRepositoryImpl): GuestRepository

    @Binds
    @Singleton
    abstract fun bindRsvpRepository(impl: RsvpRepositoryImpl): RsvpRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(impl: PaymentRepositoryImpl): PaymentRepository
}
