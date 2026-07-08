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
public final class ResetPasswordUseCase_Factory implements Factory<ResetPasswordUseCase> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public ResetPasswordUseCase_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public ResetPasswordUseCase get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static ResetPasswordUseCase_Factory create(
      Provider<AuthRepository> authRepositoryProvider) {
    return new ResetPasswordUseCase_Factory(authRepositoryProvider);
  }

  public static ResetPasswordUseCase newInstance(AuthRepository authRepository) {
    return new ResetPasswordUseCase(authRepository);
  }
}
