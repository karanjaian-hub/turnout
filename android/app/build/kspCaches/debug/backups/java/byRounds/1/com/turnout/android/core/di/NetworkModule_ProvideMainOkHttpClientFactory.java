package com.turnout.android.core.di;

import com.turnout.android.data.remote.interceptor.AuthInterceptor;
import com.turnout.android.data.remote.interceptor.TokenRefreshInterceptor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("javax.inject.Named")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class NetworkModule_ProvideMainOkHttpClientFactory implements Factory<OkHttpClient> {
  private final Provider<AuthInterceptor> authInterceptorProvider;

  private final Provider<TokenRefreshInterceptor> tokenRefreshInterceptorProvider;

  private final Provider<HttpLoggingInterceptor> loggingInterceptorProvider;

  public NetworkModule_ProvideMainOkHttpClientFactory(
      Provider<AuthInterceptor> authInterceptorProvider,
      Provider<TokenRefreshInterceptor> tokenRefreshInterceptorProvider,
      Provider<HttpLoggingInterceptor> loggingInterceptorProvider) {
    this.authInterceptorProvider = authInterceptorProvider;
    this.tokenRefreshInterceptorProvider = tokenRefreshInterceptorProvider;
    this.loggingInterceptorProvider = loggingInterceptorProvider;
  }

  @Override
  public OkHttpClient get() {
    return provideMainOkHttpClient(authInterceptorProvider.get(), tokenRefreshInterceptorProvider.get(), loggingInterceptorProvider.get());
  }

  public static NetworkModule_ProvideMainOkHttpClientFactory create(
      Provider<AuthInterceptor> authInterceptorProvider,
      Provider<TokenRefreshInterceptor> tokenRefreshInterceptorProvider,
      Provider<HttpLoggingInterceptor> loggingInterceptorProvider) {
    return new NetworkModule_ProvideMainOkHttpClientFactory(authInterceptorProvider, tokenRefreshInterceptorProvider, loggingInterceptorProvider);
  }

  public static OkHttpClient provideMainOkHttpClient(AuthInterceptor authInterceptor,
      TokenRefreshInterceptor tokenRefreshInterceptor, HttpLoggingInterceptor loggingInterceptor) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideMainOkHttpClient(authInterceptor, tokenRefreshInterceptor, loggingInterceptor));
  }
}
