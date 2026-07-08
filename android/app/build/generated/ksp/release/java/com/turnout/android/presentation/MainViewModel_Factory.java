package com.turnout.android.presentation;

import com.turnout.android.core.utils.AuthStateManager;
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
public final class MainViewModel_Factory implements Factory<MainViewModel> {
  private final Provider<AuthStateManager> authStateManagerProvider;

  public MainViewModel_Factory(Provider<AuthStateManager> authStateManagerProvider) {
    this.authStateManagerProvider = authStateManagerProvider;
  }

  @Override
  public MainViewModel get() {
    return newInstance(authStateManagerProvider.get());
  }

  public static MainViewModel_Factory create(Provider<AuthStateManager> authStateManagerProvider) {
    return new MainViewModel_Factory(authStateManagerProvider);
  }

  public static MainViewModel newInstance(AuthStateManager authStateManager) {
    return new MainViewModel(authStateManager);
  }
}
