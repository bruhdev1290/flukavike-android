package com.fluxer.client.di;

import com.fluxer.client.data.local.SecureCookieStorage;
import com.fluxer.client.data.remote.GatewayWebSocketManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.serialization.json.Json;

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
public final class NetworkModule_ProvideGatewayWebSocketManagerFactory implements Factory<GatewayWebSocketManager> {
  private final Provider<SecureCookieStorage> cookieStorageProvider;

  private final Provider<Json> jsonProvider;

  public NetworkModule_ProvideGatewayWebSocketManagerFactory(
      Provider<SecureCookieStorage> cookieStorageProvider, Provider<Json> jsonProvider) {
    this.cookieStorageProvider = cookieStorageProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public GatewayWebSocketManager get() {
    return provideGatewayWebSocketManager(cookieStorageProvider.get(), jsonProvider.get());
  }

  public static NetworkModule_ProvideGatewayWebSocketManagerFactory create(
      Provider<SecureCookieStorage> cookieStorageProvider, Provider<Json> jsonProvider) {
    return new NetworkModule_ProvideGatewayWebSocketManagerFactory(cookieStorageProvider, jsonProvider);
  }

  public static GatewayWebSocketManager provideGatewayWebSocketManager(
      SecureCookieStorage cookieStorage, Json json) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideGatewayWebSocketManager(cookieStorage, json));
  }
}
