package com.turnout.android.data.remote.interceptor;

import com.turnout.android.data.local.TokenManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class TokenRefreshInterceptor_Factory implements Factory<TokenRefreshInterceptor> {
  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<OkHttpClient> refreshClientProvider;

  private final Provider<String> baseUrlProvider;

  public TokenRefreshInterceptor_Factory(Provider<TokenManager> tokenManagerProvider,
      Provider<OkHttpClient> refreshClientProvider, Provider<String> baseUrlProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
    this.refreshClientProvider = refreshClientProvider;
    this.baseUrlProvider = baseUrlProvider;
  }

  @Override
  public TokenRefreshInterceptor get() {
    return newInstance(tokenManagerProvider.get(), refreshClientProvider.get(), baseUrlProvider.get());
  }

  public static TokenRefreshInterceptor_Factory create(Provider<TokenManager> tokenManagerProvider,
      Provider<OkHttpClient> refreshClientProvider, Provider<String> baseUrlProvider) {
    return new TokenRefreshInterceptor_Factory(tokenManagerProvider, refreshClientProvider, baseUrlProvider);
  }

  public static TokenRefreshInterceptor newInstance(TokenManager tokenManager,
      OkHttpClient refreshClient, String baseUrl) {
    return new TokenRefreshInterceptor(tokenManager, refreshClient, baseUrl);
  }
}
