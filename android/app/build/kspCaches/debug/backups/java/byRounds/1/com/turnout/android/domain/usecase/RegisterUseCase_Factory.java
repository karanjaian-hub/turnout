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
public final class RegisterUseCase_Factory implements Factory<RegisterUseCase> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public RegisterUseCase_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public RegisterUseCase get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static RegisterUseCase_Factory create(Provider<AuthRepository> authRepositoryProvider) {
    return new RegisterUseCase_Factory(authRepositoryProvider);
  }

  public static RegisterUseCase newInstance(AuthRepository authRepository) {
    return new RegisterUseCase(authRepository);
  }
}
