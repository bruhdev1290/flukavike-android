package com.fluxer.client.data.remote;

import com.fluxer.client.data.local.SecureCookieStorage;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class GatewayWebSocketManager_Factory implements Factory<GatewayWebSocketManager> {
  private final Provider<SecureCookieStorage> cookieStorageProvider;

  private final Provider<Json> jsonProvider;

  public GatewayWebSocketManager_Factory(Provider<SecureCookieStorage> cookieStorageProvider,
      Provider<Json> jsonProvider) {
    this.cookieStorageProvider = cookieStorageProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public GatewayWebSocketManager get() {
    return newInstance(cookieStorageProvider.get(), jsonProvider.get());
  }

  public static GatewayWebSocketManager_Factory create(
      Provider<SecureCookieStorage> cookieStorageProvider, Provider<Json> jsonProvider) {
    return new GatewayWebSocketManager_Factory(cookieStorageProvider, jsonProvider);
  }

  public static GatewayWebSocketManager newInstance(SecureCookieStorage cookieStorage, Json json) {
    return new GatewayWebSocketManager(cookieStorage, json);
  }
}
