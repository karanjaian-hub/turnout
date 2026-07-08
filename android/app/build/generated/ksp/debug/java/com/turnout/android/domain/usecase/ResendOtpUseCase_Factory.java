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
public final class ResendOtpUseCase_Factory implements Factory<ResendOtpUseCase> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public ResendOtpUseCase_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public ResendOtpUseCase get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static ResendOtpUseCase_Factory create(Provider<AuthRepository> authRepositoryProvider) {
    return new ResendOtpUseCase_Factory(authRepositoryProvider);
  }

  public static ResendOtpUseCase newInstance(AuthRepository authRepository) {
    return new ResendOtpUseCase(authRepository);
  }
}
