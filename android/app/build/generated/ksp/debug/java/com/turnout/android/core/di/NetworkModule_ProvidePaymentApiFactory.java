package com.turnout.android.core.di;

import com.turnout.android.data.remote.api.PaymentApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("javax.inject.Named")
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
public final class NetworkModule_ProvidePaymentApiFactory implements Factory<PaymentApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvidePaymentApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public PaymentApi get() {
    return providePaymentApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvidePaymentApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvidePaymentApiFactory(retrofitProvider);
  }

  public static PaymentApi providePaymentApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.providePaymentApi(retrofit));
  }
}
