package com.turnout.android.presentation.auth;

import com.turnout.android.domain.usecase.GetCurrentUserUseCase;
import com.turnout.android.domain.usecase.LoginUseCase;
import com.turnout.android.domain.usecase.LogoutUseCase;
import com.turnout.android.domain.usecase.ResendOtpUseCase;
import com.turnout.android.domain.usecase.VerifyOtpUseCase;
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
public final class AuthViewModel_Factory implements Factory<AuthViewModel> {
  private final Provider<LoginUseCase> loginUseCaseProvider;

  private final Provider<VerifyOtpUseCase> verifyOtpUseCaseProvider;

  private final Provider<GetCurrentUserUseCase> getCurrentUserUseCaseProvider;

  private final Provider<LogoutUseCase> logoutUseCaseProvider;

  private final Provider<ResendOtpUseCase> resendOtpUseCaseProvider;

  public AuthViewModel_Factory(Provider<LoginUseCase> loginUseCaseProvider,
      Provider<VerifyOtpUseCase> verifyOtpUseCaseProvider,
      Provider<GetCurrentUserUseCase> getCurrentUserUseCaseProvider,
      Provider<LogoutUseCase> logoutUseCaseProvider,
      Provider<ResendOtpUseCase> resendOtpUseCaseProvider) {
    this.loginUseCaseProvider = loginUseCaseProvider;
    this.verifyOtpUseCaseProvider = verifyOtpUseCaseProvider;
    this.getCurrentUserUseCaseProvider = getCurrentUserUseCaseProvider;
    this.logoutUseCaseProvider = logoutUseCaseProvider;
    this.resendOtpUseCaseProvider = resendOtpUseCaseProvider;
  }

  @Override
  public AuthViewModel get() {
    return newInstance(loginUseCaseProvider.get(), verifyOtpUseCaseProvider.get(), getCurrentUserUseCaseProvider.get(), logoutUseCaseProvider.get(), resendOtpUseCaseProvider.get());
  }

  public static AuthViewModel_Factory create(Provider<LoginUseCase> loginUseCaseProvider,
      Provider<VerifyOtpUseCase> verifyOtpUseCaseProvider,
      Provider<GetCurrentUserUseCase> getCurrentUserUseCaseProvider,
      Provider<LogoutUseCase> logoutUseCaseProvider,
      Provider<ResendOtpUseCase> resendOtpUseCaseProvider) {
    return new AuthViewModel_Factory(loginUseCaseProvider, verifyOtpUseCaseProvider, getCurrentUserUseCaseProvider, logoutUseCaseProvider, resendOtpUseCaseProvider);
  }

  public static AuthViewModel newInstance(LoginUseCase loginUseCase,
      VerifyOtpUseCase verifyOtpUseCase, GetCurrentUserUseCase getCurrentUserUseCase,
      LogoutUseCase logoutUseCase, ResendOtpUseCase resendOtpUseCase) {
    return new AuthViewModel(loginUseCase, verifyOtpUseCase, getCurrentUserUseCase, logoutUseCase, resendOtpUseCase);
  }
}
