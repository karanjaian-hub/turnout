package com.turnout.android.core.di;

import com.turnout.android.data.remote.api.AiApi;
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
public final class NetworkModule_ProvideAiApiFactory implements Factory<AiApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideAiApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public AiApi get() {
    return provideAiApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideAiApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideAiApiFactory(retrofitProvider);
  }

  public static AiApi provideAiApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideAiApi(retrofit));
  }
}
