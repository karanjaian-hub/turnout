package com.turnout.android.core.di;

import com.turnout.android.data.remote.api.RsvpApi;
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
public final class NetworkModule_ProvideRsvpApiFactory implements Factory<RsvpApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideRsvpApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public RsvpApi get() {
    return provideRsvpApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideRsvpApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideRsvpApiFactory(retrofitProvider);
  }

  public static RsvpApi provideRsvpApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideRsvpApi(retrofit));
  }
}
