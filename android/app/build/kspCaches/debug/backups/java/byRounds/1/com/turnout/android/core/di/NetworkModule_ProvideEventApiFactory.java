package com.turnout.android.core.di;

import com.turnout.android.data.remote.api.EventApi;
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
public final class NetworkModule_ProvideEventApiFactory implements Factory<EventApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideEventApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public EventApi get() {
    return provideEventApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideEventApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideEventApiFactory(retrofitProvider);
  }

  public static EventApi provideEventApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideEventApi(retrofit));
  }
}
