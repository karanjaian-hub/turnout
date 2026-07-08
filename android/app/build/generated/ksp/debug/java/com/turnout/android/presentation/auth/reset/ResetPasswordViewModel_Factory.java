package com.turnout.android.presentation.auth.reset;

import com.turnout.android.domain.usecase.ResetPasswordUseCase;
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
public final class ResetPasswordViewModel_Factory implements Factory<ResetPasswordViewModel> {
  private final Provider<ResetPasswordUseCase> resetPasswordUseCaseProvider;

  public ResetPasswordViewModel_Factory(
      Provider<ResetPasswordUseCase> resetPasswordUseCaseProvider) {
    this.resetPasswordUseCaseProvider = resetPasswordUseCaseProvider;
  }

  @Override
  public ResetPasswordViewModel get() {
    return newInstance(resetPasswordUseCaseProvider.get());
  }

  public static ResetPasswordViewModel_Factory create(
      Provider<ResetPasswordUseCase> resetPasswordUseCaseProvider) {
    return new ResetPasswordViewModel_Factory(resetPasswordUseCaseProvider);
  }

  public static ResetPasswordViewModel newInstance(ResetPasswordUseCase resetPasswordUseCase) {
    return new ResetPasswordViewModel(resetPasswordUseCase);
  }
}
