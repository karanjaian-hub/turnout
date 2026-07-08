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
public final class SaveFcmTokenUseCase_Factory implements Factory<SaveFcmTokenUseCase> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public SaveFcmTokenUseCase_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public SaveFcmTokenUseCase get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static SaveFcmTokenUseCase_Factory create(
      Provider<AuthRepository> authRepositoryProvider) {
    return new SaveFcmTokenUseCase_Factory(authRepositoryProvider);
  }

  public static SaveFcmTokenUseCase newInstance(AuthRepository authRepository) {
    return new SaveFcmTokenUseCase(authRepository);
  }
}
