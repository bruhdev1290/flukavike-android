package com.fluxer.client.di;

import com.fluxer.client.data.remote.FluxerApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

@ScopeMetadata("javax.inject.Singleton")
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
    "KotlinInternalInJava"
})
public final class NetworkModule_ProvideFluxerApiServiceFactory implements Factory<FluxerApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideFluxerApiServiceFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public FluxerApiService get() {
    return provideFluxerApiService(retrofitProvider.get());
  }

  public static NetworkModule_ProvideFluxerApiServiceFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideFluxerApiServiceFactory(retrofitProvider);
  }

  public static FluxerApiService provideFluxerApiService(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideFluxerApiService(retrofit));
  }
}
