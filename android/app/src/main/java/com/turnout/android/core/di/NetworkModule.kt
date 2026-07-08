package com.turnout.android.core.di

import com.turnout.android.BuildConfig
import com.turnout.android.core.utils.ConnectivityObserver
import com.turnout.android.data.local.TokenManager
import com.turnout.android.data.remote.api.*
import com.turnout.android.data.remote.interceptor.AuthInterceptor
import com.turnout.android.data.remote.interceptor.TokenRefreshInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Named("baseUrl")
    fun provideBaseUrl(): String = BuildConfig.API_BASE_URL

    @Provides
    @Singleton
    fun provideConnectivityObserver(
        @ApplicationContext context: android.content.Context
    ): ConnectivityObserver = ConnectivityObserver(context)

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            // Log full request/response bodies in debug only — never in release builds
            level = if (BuildConfig.ENABLE_HTTP_LOGGING)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

    // Bare client used exclusively by TokenRefreshInterceptor.
    // No auth interceptor here — prevents the infinite 401 -> refresh -> 401 loop.
    @Provides
    @Singleton
    @Named("refreshClient")
    fun provideRefreshClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    // Authenticated client — attaches tokens, handles 401 refresh. Used for every
    // endpoint that requires a logged-in user (events, guests, dashboard, settings, etc.).
    @Provides
    @Singleton
    @Named("mainClient")
    fun provideMainOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenRefreshInterceptor: TokenRefreshInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            // Order matters: auth attaches token -> server responds -> refresh handles 401
            .addInterceptor(authInterceptor)
            .addInterceptor(tokenRefreshInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    // Public client — deliberately NO auth interceptors at all. A guest tapping an RSVP
    // link from an email has no login and no token; attaching one (or trying to refresh
    // on a 401) would be meaningless and could even leak an unrelated logged-in admin's
    // token onto a request that should be anonymous.
    @Provides
    @Singleton
    @Named("publicClient")
    fun providePublicOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @Named("mainRetrofit")
    fun provideMainRetrofit(
        @Named("mainClient") okHttpClient: OkHttpClient,
        @Named("baseUrl") baseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @Named("publicRetrofit")
    fun providePublicRetrofit(
        @Named("publicClient") okHttpClient: OkHttpClient,
        @Named("baseUrl") baseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    // AuthApi and EmailApi use mainRetrofit — both require a logged-in admin.
    @Provides @Singleton
    fun provideAuthApi(@Named("mainRetrofit") retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides @Singleton
    fun provideEmailApi(@Named("mainRetrofit") retrofit: Retrofit): EmailApi =
        retrofit.create(EmailApi::class.java)

    @Provides @Singleton
    fun provideEventApi(@Named("mainRetrofit") retrofit: Retrofit): EventApi =
        retrofit.create(EventApi::class.java)

    @Provides @Singleton
    fun provideGuestApi(@Named("mainRetrofit") retrofit: Retrofit): GuestApi =
        retrofit.create(GuestApi::class.java)

    @Provides @Singleton
    fun provideAiApi(@Named("mainRetrofit") retrofit: Retrofit): AiApi =
        retrofit.create(AiApi::class.java)

    @Provides @Singleton
    fun providePaymentApi(@Named("mainRetrofit") retrofit: Retrofit): PaymentApi =
        retrofit.create(PaymentApi::class.java)

    // RsvpApi uses publicRetrofit — the one genuinely tokenless endpoint group.
    @Provides @Singleton
    fun provideRsvpApi(@Named("publicRetrofit") retrofit: Retrofit): RsvpApi =
        retrofit.create(RsvpApi::class.java)
}
