package com.turnout.android.domain.usecase;

import com.turnout.android.domain.repository.AuthRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class RefreshTokenUseCase_Factory implements Factory<RefreshTokenUseCase> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public RefreshTokenUseCase_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public RefreshTokenUseCase get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static RefreshTokenUseCase_Factory create(
      Provider<AuthRepository> authRepositoryProvider) {
    return new RefreshTokenUseCase_Factory(authRepositoryProvider);
  }

  public static RefreshTokenUseCase newInstance(AuthRepository authRepository) {
    return new RefreshTokenUseCase(authRepository);
  }
}
