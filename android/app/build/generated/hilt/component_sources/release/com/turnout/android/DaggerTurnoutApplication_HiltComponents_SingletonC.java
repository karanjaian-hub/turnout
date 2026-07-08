package com.turnout.android;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.turnout.android.core.di.NetworkModule_ProvideAuthApiFactory;
import com.turnout.android.core.di.NetworkModule_ProvideBaseUrlFactory;
import com.turnout.android.core.di.NetworkModule_ProvideLoggingInterceptorFactory;
import com.turnout.android.core.di.NetworkModule_ProvideMainOkHttpClientFactory;
import com.turnout.android.core.di.NetworkModule_ProvideMainRetrofitFactory;
import com.turnout.android.core.di.NetworkModule_ProvideRefreshClientFactory;
import com.turnout.android.core.utils.AuthStateManager;
import com.turnout.android.data.local.TokenManager;
import com.turnout.android.data.remote.api.AuthApi;
import com.turnout.android.data.remote.interceptor.AuthInterceptor;
import com.turnout.android.data.remote.interceptor.TokenRefreshInterceptor;
import com.turnout.android.data.repository.AuthRepositoryImpl;
import com.turnout.android.domain.repository.AuthRepository;
import com.turnout.android.domain.usecase.ForgotPasswordUseCase;
import com.turnout.android.domain.usecase.GetCurrentUserUseCase;
import com.turnout.android.domain.usecase.LoginUseCase;
import com.turnout.android.domain.usecase.LogoutUseCase;
import com.turnout.android.domain.usecase.RefreshTokenUseCase;
import com.turnout.android.domain.usecase.RegisterUseCase;
import com.turnout.android.domain.usecase.ResendOtpUseCase;
import com.turnout.android.domain.usecase.ResetPasswordUseCase;
import com.turnout.android.domain.usecase.VerifyOtpUseCase;
import com.turnout.android.presentation.MainViewModel;
import com.turnout.android.presentation.MainViewModel_HiltModules;
import com.turnout.android.presentation.auth.AuthViewModel;
import com.turnout.android.presentation.auth.AuthViewModel_HiltModules;
import com.turnout.android.presentation.auth.forgot.ForgotPasswordViewModel;
import com.turnout.android.presentation.auth.forgot.ForgotPasswordViewModel_HiltModules;
import com.turnout.android.presentation.auth.register.RegisterViewModel;
import com.turnout.android.presentation.auth.register.RegisterViewModel_HiltModules;
import com.turnout.android.presentation.auth.reset.ResetPasswordViewModel;
import com.turnout.android.presentation.auth.reset.ResetPasswordViewModel_HiltModules;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

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
public final class DaggerTurnoutApplication_HiltComponents_SingletonC {
  private DaggerTurnoutApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public TurnoutApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements TurnoutApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public TurnoutApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements TurnoutApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public TurnoutApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements TurnoutApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public TurnoutApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements TurnoutApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public TurnoutApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements TurnoutApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public TurnoutApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements TurnoutApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public TurnoutApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements TurnoutApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public TurnoutApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends TurnoutApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends TurnoutApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends TurnoutApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends TurnoutApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(5).put(LazyClassKeyProvider.com_turnout_android_presentation_auth_AuthViewModel, AuthViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_turnout_android_presentation_auth_forgot_ForgotPasswordViewModel, ForgotPasswordViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_turnout_android_presentation_MainViewModel, MainViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_turnout_android_presentation_auth_register_RegisterViewModel, RegisterViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_turnout_android_presentation_auth_reset_ResetPasswordViewModel, ResetPasswordViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_turnout_android_presentation_auth_AuthViewModel = "com.turnout.android.presentation.auth.AuthViewModel";

      static String com_turnout_android_presentation_auth_forgot_ForgotPasswordViewModel = "com.turnout.android.presentation.auth.forgot.ForgotPasswordViewModel";

      static String com_turnout_android_presentation_auth_register_RegisterViewModel = "com.turnout.android.presentation.auth.register.RegisterViewModel";

      static String com_turnout_android_presentation_MainViewModel = "com.turnout.android.presentation.MainViewModel";

      static String com_turnout_android_presentation_auth_reset_ResetPasswordViewModel = "com.turnout.android.presentation.auth.reset.ResetPasswordViewModel";

      @KeepFieldType
      AuthViewModel com_turnout_android_presentation_auth_AuthViewModel2;

      @KeepFieldType
      ForgotPasswordViewModel com_turnout_android_presentation_auth_forgot_ForgotPasswordViewModel2;

      @KeepFieldType
      RegisterViewModel com_turnout_android_presentation_auth_register_RegisterViewModel2;

      @KeepFieldType
      MainViewModel com_turnout_android_presentation_MainViewModel2;

      @KeepFieldType
      ResetPasswordViewModel com_turnout_android_presentation_auth_reset_ResetPasswordViewModel2;
    }
  }

  private static final class ViewModelCImpl extends TurnoutApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AuthViewModel> authViewModelProvider;

    private Provider<ForgotPasswordViewModel> forgotPasswordViewModelProvider;

    private Provider<MainViewModel> mainViewModelProvider;

    private Provider<RegisterViewModel> registerViewModelProvider;

    private Provider<ResetPasswordViewModel> resetPasswordViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    private LoginUseCase loginUseCase() {
      return new LoginUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private VerifyOtpUseCase verifyOtpUseCase() {
      return new VerifyOtpUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private GetCurrentUserUseCase getCurrentUserUseCase() {
      return new GetCurrentUserUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private LogoutUseCase logoutUseCase() {
      return new LogoutUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private ResendOtpUseCase resendOtpUseCase() {
      return new ResendOtpUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private ForgotPasswordUseCase forgotPasswordUseCase() {
      return new ForgotPasswordUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private RegisterUseCase registerUseCase() {
      return new RegisterUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private ResetPasswordUseCase resetPasswordUseCase() {
      return new ResetPasswordUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.authViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.forgotPasswordViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.mainViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.registerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.resetPasswordViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(5).put(LazyClassKeyProvider.com_turnout_android_presentation_auth_AuthViewModel, ((Provider) authViewModelProvider)).put(LazyClassKeyProvider.com_turnout_android_presentation_auth_forgot_ForgotPasswordViewModel, ((Provider) forgotPasswordViewModelProvider)).put(LazyClassKeyProvider.com_turnout_android_presentation_MainViewModel, ((Provider) mainViewModelProvider)).put(LazyClassKeyProvider.com_turnout_android_presentation_auth_register_RegisterViewModel, ((Provider) registerViewModelProvider)).put(LazyClassKeyProvider.com_turnout_android_presentation_auth_reset_ResetPasswordViewModel, ((Provider) resetPasswordViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_turnout_android_presentation_MainViewModel = "com.turnout.android.presentation.MainViewModel";

      static String com_turnout_android_presentation_auth_forgot_ForgotPasswordViewModel = "com.turnout.android.presentation.auth.forgot.ForgotPasswordViewModel";

      static String com_turnout_android_presentation_auth_register_RegisterViewModel = "com.turnout.android.presentation.auth.register.RegisterViewModel";

      static String com_turnout_android_presentation_auth_AuthViewModel = "com.turnout.android.presentation.auth.AuthViewModel";

      static String com_turnout_android_presentation_auth_reset_ResetPasswordViewModel = "com.turnout.android.presentation.auth.reset.ResetPasswordViewModel";

      @KeepFieldType
      MainViewModel com_turnout_android_presentation_MainViewModel2;

      @KeepFieldType
      ForgotPasswordViewModel com_turnout_android_presentation_auth_forgot_ForgotPasswordViewModel2;

      @KeepFieldType
      RegisterViewModel com_turnout_android_presentation_auth_register_RegisterViewModel2;

      @KeepFieldType
      AuthViewModel com_turnout_android_presentation_auth_AuthViewModel2;

      @KeepFieldType
      ResetPasswordViewModel com_turnout_android_presentation_auth_reset_ResetPasswordViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.turnout.android.presentation.auth.AuthViewModel 
          return (T) new AuthViewModel(viewModelCImpl.loginUseCase(), viewModelCImpl.verifyOtpUseCase(), viewModelCImpl.getCurrentUserUseCase(), viewModelCImpl.logoutUseCase(), viewModelCImpl.resendOtpUseCase());

          case 1: // com.turnout.android.presentation.auth.forgot.ForgotPasswordViewModel 
          return (T) new ForgotPasswordViewModel(viewModelCImpl.forgotPasswordUseCase());

          case 2: // com.turnout.android.presentation.MainViewModel 
          return (T) new MainViewModel(singletonCImpl.authStateManagerProvider.get());

          case 3: // com.turnout.android.presentation.auth.register.RegisterViewModel 
          return (T) new RegisterViewModel(viewModelCImpl.registerUseCase());

          case 4: // com.turnout.android.presentation.auth.reset.ResetPasswordViewModel 
          return (T) new ResetPasswordViewModel(viewModelCImpl.resetPasswordUseCase());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends TurnoutApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends TurnoutApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends TurnoutApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<TokenManager> tokenManagerProvider;

    private Provider<HttpLoggingInterceptor> provideLoggingInterceptorProvider;

    private Provider<OkHttpClient> provideRefreshClientProvider;

    private Provider<TokenRefreshInterceptor> tokenRefreshInterceptorProvider;

    private Provider<OkHttpClient> provideMainOkHttpClientProvider;

    private Provider<Retrofit> provideMainRetrofitProvider;

    private Provider<AuthApi> provideAuthApiProvider;

    private Provider<AuthRepositoryImpl> authRepositoryImplProvider;

    private Provider<AuthRepository> bindAuthRepositoryProvider;

    private Provider<AuthStateManager> authStateManagerProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private AuthInterceptor authInterceptor() {
      return new AuthInterceptor(tokenManagerProvider.get());
    }

    private RefreshTokenUseCase refreshTokenUseCase() {
      return new RefreshTokenUseCase(bindAuthRepositoryProvider.get());
    }

    private GetCurrentUserUseCase getCurrentUserUseCase() {
      return new GetCurrentUserUseCase(bindAuthRepositoryProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.tokenManagerProvider = DoubleCheck.provider(new SwitchingProvider<TokenManager>(singletonCImpl, 4));
      this.provideLoggingInterceptorProvider = DoubleCheck.provider(new SwitchingProvider<HttpLoggingInterceptor>(singletonCImpl, 7));
      this.provideRefreshClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 6));
      this.tokenRefreshInterceptorProvider = DoubleCheck.provider(new SwitchingProvider<TokenRefreshInterceptor>(singletonCImpl, 5));
      this.provideMainOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 3));
      this.provideMainRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 2));
      this.provideAuthApiProvider = DoubleCheck.provider(new SwitchingProvider<AuthApi>(singletonCImpl, 1));
      this.authRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 0);
      this.bindAuthRepositoryProvider = DoubleCheck.provider((Provider) authRepositoryImplProvider);
      this.authStateManagerProvider = DoubleCheck.provider(new SwitchingProvider<AuthStateManager>(singletonCImpl, 8));
    }

    @Override
    public void injectTurnoutApplication(TurnoutApplication turnoutApplication) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.turnout.android.data.repository.AuthRepositoryImpl 
          return (T) new AuthRepositoryImpl(singletonCImpl.provideAuthApiProvider.get(), singletonCImpl.tokenManagerProvider.get());

          case 1: // com.turnout.android.data.remote.api.AuthApi 
          return (T) NetworkModule_ProvideAuthApiFactory.provideAuthApi(singletonCImpl.provideMainRetrofitProvider.get());

          case 2: // @javax.inject.Named("mainRetrofit") retrofit2.Retrofit 
          return (T) NetworkModule_ProvideMainRetrofitFactory.provideMainRetrofit(singletonCImpl.provideMainOkHttpClientProvider.get(), NetworkModule_ProvideBaseUrlFactory.provideBaseUrl());

          case 3: // @javax.inject.Named("mainClient") okhttp3.OkHttpClient 
          return (T) NetworkModule_ProvideMainOkHttpClientFactory.provideMainOkHttpClient(singletonCImpl.authInterceptor(), singletonCImpl.tokenRefreshInterceptorProvider.get(), singletonCImpl.provideLoggingInterceptorProvider.get());

          case 4: // com.turnout.android.data.local.TokenManager 
          return (T) new TokenManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 5: // com.turnout.android.data.remote.interceptor.TokenRefreshInterceptor 
          return (T) new TokenRefreshInterceptor(singletonCImpl.tokenManagerProvider.get(), singletonCImpl.provideRefreshClientProvider.get(), NetworkModule_ProvideBaseUrlFactory.provideBaseUrl());

          case 6: // @javax.inject.Named("refreshClient") okhttp3.OkHttpClient 
          return (T) NetworkModule_ProvideRefreshClientFactory.provideRefreshClient(singletonCImpl.provideLoggingInterceptorProvider.get());

          case 7: // okhttp3.logging.HttpLoggingInterceptor 
          return (T) NetworkModule_ProvideLoggingInterceptorFactory.provideLoggingInterceptor();

          case 8: // com.turnout.android.core.utils.AuthStateManager 
          return (T) new AuthStateManager(singletonCImpl.tokenManagerProvider.get(), singletonCImpl.refreshTokenUseCase(), singletonCImpl.getCurrentUserUseCase());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
