package com.turnout.android.core.utils;

import com.turnout.android.data.local.TokenManager;
import com.turnout.android.domain.usecase.GetCurrentUserUseCase;
import com.turnout.android.domain.usecase.RefreshTokenUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class AuthStateManager_Factory implements Factory<AuthStateManager> {
  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<RefreshTokenUseCase> refreshTokenUseCaseProvider;

  private final Provider<GetCurrentUserUseCase> getCurrentUserUseCaseProvider;

  public AuthStateManager_Factory(Provider<TokenManager> tokenManagerProvider,
      Provider<RefreshTokenUseCase> refreshTokenUseCaseProvider,
      Provider<GetCurrentUserUseCase> getCurrentUserUseCaseProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
    this.refreshTokenUseCaseProvider = refreshTokenUseCaseProvider;
    this.getCurrentUserUseCaseProvider = getCurrentUserUseCaseProvider;
  }

  @Override
  public AuthStateManager get() {
    return newInstance(tokenManagerProvider.get(), refreshTokenUseCaseProvider.get(), getCurrentUserUseCaseProvider.get());
  }

  public static AuthStateManager_Factory create(Provider<TokenManager> tokenManagerProvider,
      Provider<RefreshTokenUseCase> refreshTokenUseCaseProvider,
      Provider<GetCurrentUserUseCase> getCurrentUserUseCaseProvider) {
    return new AuthStateManager_Factory(tokenManagerProvider, refreshTokenUseCaseProvider, getCurrentUserUseCaseProvider);
  }

  public static AuthStateManager newInstance(TokenManager tokenManager,
      RefreshTokenUseCase refreshTokenUseCase, GetCurrentUserUseCase getCurrentUserUseCase) {
    return new AuthStateManager(tokenManager, refreshTokenUseCase, getCurrentUserUseCase);
  }
}
