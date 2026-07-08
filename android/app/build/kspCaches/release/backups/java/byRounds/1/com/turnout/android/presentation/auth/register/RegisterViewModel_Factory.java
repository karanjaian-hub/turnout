package com.turnout.android.presentation.auth.register;

import com.turnout.android.domain.usecase.RegisterUseCase;
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
public final class RegisterViewModel_Factory implements Factory<RegisterViewModel> {
  private final Provider<RegisterUseCase> registerUseCaseProvider;

  public RegisterViewModel_Factory(Provider<RegisterUseCase> registerUseCaseProvider) {
    this.registerUseCaseProvider = registerUseCaseProvider;
  }

  @Override
  public RegisterViewModel get() {
    return newInstance(registerUseCaseProvider.get());
  }

  public static RegisterViewModel_Factory create(
      Provider<RegisterUseCase> registerUseCaseProvider) {
    return new RegisterViewModel_Factory(registerUseCaseProvider);
  }

  public static RegisterViewModel newInstance(RegisterUseCase registerUseCase) {
    return new RegisterViewModel(registerUseCase);
  }
}
